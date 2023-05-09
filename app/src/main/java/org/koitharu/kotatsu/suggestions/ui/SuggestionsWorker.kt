package org.koitharu.kotatsu.suggestions.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.FloatRange
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.suggestions.domain.MangaSuggestion
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.utils.ext.asArrayList
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import org.koitharu.kotatsu.utils.ext.trySetForeground
import java.util.concurrent.TimeUnit
import kotlin.math.pow

@HiltWorker
class SuggestionsWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val suggestionRepository: SuggestionRepository,
	private val historyRepository: HistoryRepository,
	private val appSettings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		val count = doWorkImpl()
		val outputData = workDataOf(DATA_COUNT to count)
		return Result.success(outputData)
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		val title = applicationContext.getString(R.string.suggestions_updating)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				WORKER_CHANNEL_ID,
				title,
				NotificationManager.IMPORTANCE_LOW,
			)
			channel.setShowBadge(false)
			channel.enableVibration(false)
			channel.setSound(null, null)
			channel.enableLights(false)
			manager.createNotificationChannel(channel)
		}

		val notification = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setDefaults(0)
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.build()

		return ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
	}

	private suspend fun doWorkImpl(): Int {
		if (!appSettings.isSuggestionsEnabled) {
			suggestionRepository.clear()
			return 0
		}
		val blacklistTagRegex = appSettings.getSuggestionsTagsBlacklistRegex()
		val allTags = historyRepository.getPopularTags(TAGS_LIMIT).filterNot {
			blacklistTagRegex?.containsMatchIn(it.title) ?: false
		}
		if (allTags.isEmpty()) {
			return 0
		}
		if (TAG in tags) { // not expedited
			trySetForeground()
		}
		val tagsBySources = allTags.groupBy { x -> x.source }
		val dispatcher = Dispatchers.Default.limitedParallelism(MAX_PARALLELISM)
		val rawResults = coroutineScope {
			tagsBySources.flatMap { (source, tags) ->
				val repo = mangaRepositoryFactory.tryCreate(source) ?: return@flatMap emptyList()
				tags.map { tag ->
					async(dispatcher) {
						repo.getListSafe(tag)
					}
				}
			}.awaitAll().flatten().asArrayList()
		}
		if (appSettings.isSuggestionsExcludeNsfw) {
			rawResults.removeAll { it.isNsfw }
		}
		if (blacklistTagRegex != null) {
			rawResults.removeAll {
				it.tags.any { x -> blacklistTagRegex.containsMatchIn(x.title) }
			}
		}
		if (rawResults.isEmpty()) {
			return 0
		}
		val suggestions = rawResults.distinctBy { manga ->
			manga.id
		}.map { manga ->
			MangaSuggestion(
				manga = manga,
				relevance = computeRelevance(manga.tags, allTags),
			)
		}.sortedBy { it.relevance }.take(LIMIT)
		suggestionRepository.replace(suggestions)
		return suggestions.size
	}

	@FloatRange(from = 0.0, to = 1.0)
	private fun computeRelevance(mangaTags: Set<MangaTag>, allTags: List<MangaTag>): Float {
		val maxWeight = (allTags.size + allTags.size + 1 - mangaTags.size) * mangaTags.size / 2.0
		val weight = mangaTags.sumOf { tag ->
			val index = allTags.indexOf(tag)
			if (index < 0) 0 else allTags.size - index
		}
		return (weight / maxWeight).pow(2.0).toFloat()
	}

	private suspend fun MangaRepository.getListSafe(tag: MangaTag) = runCatchingCancellable {
		getList(offset = 0, sortOrder = SortOrder.UPDATED, tags = setOf(tag))
	}.onFailure { error ->
		error.printStackTraceDebug()
	}.getOrDefault(emptyList())

	private fun MangaRepository.Factory.tryCreate(source: MangaSource) = runCatching {
		create(source)
	}.onFailure { error ->
		error.printStackTraceDebug()
	}.getOrNull()

	companion object {

		private const val TAG = "suggestions"
		private const val TAG_ONESHOT = "suggestions_oneshot"
		private const val LIMIT = 140
		private const val TAGS_LIMIT = 20
		private const val MAX_PARALLELISM = 4
		private const val DATA_COUNT = "count"
		private const val WORKER_CHANNEL_ID = "suggestion_worker"
		private const val WORKER_NOTIFICATION_ID = 36

		fun setup(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.UNMETERED)
				.setRequiresBatteryNotLow(true)
				.build()
			val request = PeriodicWorkRequestBuilder<SuggestionsWorker>(6, TimeUnit.HOURS)
				.setConstraints(constraints)
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
				.build()
			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
		}

		fun startNow(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			val request = OneTimeWorkRequestBuilder<SuggestionsWorker>()
				.setConstraints(constraints)
				.addTag(TAG_ONESHOT)
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
				.build()
			WorkManager.getInstance(context)
				.enqueue(request)
		}
	}
}
