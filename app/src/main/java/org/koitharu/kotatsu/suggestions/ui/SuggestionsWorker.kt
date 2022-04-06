package org.koitharu.kotatsu.suggestions.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.suggestions.domain.MangaSuggestion
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository

class SuggestionsWorker(appContext: Context, params: WorkerParameters) :
	CoroutineWorker(appContext, params), KoinComponent {

	private val suggestionRepository by inject<SuggestionRepository>()
	private val historyRepository by inject<HistoryRepository>()
	private val appSettings by inject<AppSettings>()

	override suspend fun doWork(): Result = try {
		val count = doWorkImpl()
		Result.success(workDataOf(DATA_COUNT to count))
	} catch (t: Throwable) {
		Result.failure()
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		val title = applicationContext.getString(R.string.suggestions_updating)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				WORKER_CHANNEL_ID,
				title,
				NotificationManager.IMPORTANCE_LOW
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
			.setDefaults(0)
			.setColor(ContextCompat.getColor(applicationContext, R.color.blue_primary_dark))
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.setOngoing(true)
			.build()

		return ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
	}

	private suspend fun doWorkImpl(): Int {
		if (!appSettings.isSuggestionsEnabled) {
			suggestionRepository.clear()
			return 0
		}
		val rawResults = ArrayList<Manga>()
		val allTags = historyRepository.getAllTags()
		if (allTags.isEmpty()) {
			return 0
		}
		val tagsBySources = allTags.groupBy { x -> x.source }
		for ((source, tags) in tagsBySources) {
			val repo = MangaRepository(source)
			tags.flatMapTo(rawResults) { tag ->
				repo.getList(
					offset = 0,
					sortOrder = SortOrder.UPDATED,
					tags = setOf(tag),
				)
			}
		}
		if (appSettings.isSuggestionsExcludeNsfw) {
			rawResults.removeAll { it.isNsfw }
		}
		if (rawResults.isEmpty()) {
			return 0
		}
		val suggestions = rawResults.distinctBy { manga ->
			manga.id
		}.map { manga ->
			val jointTags = manga.tags intersect allTags
			MangaSuggestion(
				manga = manga,
				relevance = (jointTags.size / manga.tags.size.toDouble()).pow(2.0).toFloat(),
			)
		}.sortedBy { it.relevance }.take(LIMIT)
		suggestionRepository.replace(suggestions)
		return suggestions.size
	}

	companion object {

		private const val TAG = "suggestions"
		private const val TAG_ONESHOT = "suggestions_oneshot"
		private const val LIMIT = 140
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