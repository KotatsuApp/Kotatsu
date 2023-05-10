package org.koitharu.kotatsu.suggestions.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.FloatRange
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.parseAsHtml
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
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.distinctById
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.suggestions.domain.MangaSuggestion
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.suggestions.domain.TagsBlacklist
import org.koitharu.kotatsu.utils.ext.almostEquals
import org.koitharu.kotatsu.utils.ext.asArrayList
import org.koitharu.kotatsu.utils.ext.flatten
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import org.koitharu.kotatsu.utils.ext.takeMostFrequent
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import org.koitharu.kotatsu.utils.ext.trySetForeground
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.random.Random

@HiltWorker
class SuggestionsWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val coil: ImageLoader,
	private val suggestionRepository: SuggestionRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val appSettings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		trySetForeground()
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
		val seed = (
			historyRepository.getList(0, 20) +
				favouritesRepository.getLastManga(20)
			).distinctById()
		val sources = appSettings.getMangaSources(includeHidden = false)
		if (seed.isEmpty() || sources.isEmpty()) {
			return 0
		}
		val tagsBlacklist = TagsBlacklist(appSettings.suggestionsTagsBlacklist, TAG_EQ_THRESHOLD)
		val tags = seed.flatMap { it.tags.map { x -> x.title } }.takeMostFrequent(10)

		val producer = channelFlow {
			for (it in sources.shuffled()) {
				launch {
					send(getList(it, tags, tagsBlacklist))
				}
			}
		}
		val suggestions = producer
			.flatten()
			.take(MAX_RAW_RESULTS)
			.map { manga ->
				MangaSuggestion(
					manga = manga,
					relevance = computeRelevance(manga.tags, tags),
				)
			}.toList()
			.sortedBy { it.relevance }
			.take(MAX_RESULTS)
		suggestionRepository.replace(suggestions)
		if (appSettings.isSuggestionsNotificationAvailable) {
			runCatchingCancellable {
				val manga = suggestions[Random.nextInt(0, suggestions.size / 3)]
				val details = mangaRepositoryFactory.create(manga.manga.source)
					.getDetails(manga.manga)
				if (details !in tagsBlacklist) {
					showNotification(details)
				}
			}.onFailure {
				it.printStackTraceDebug()
			}
		}
		return suggestions.size
	}

	private suspend fun getList(
		source: MangaSource,
		tags: List<String>,
		blacklist: TagsBlacklist,
	): List<Manga> = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(source)
		val availableOrders = repository.sortOrders
		val order = preferredSortOrders.first { it in availableOrders }
		val availableTags = repository.getTags()
		val tag = tags.firstNotNullOfOrNull { title ->
			availableTags.find { x -> x.title.almostEquals(title, TAG_EQ_THRESHOLD) }
		}
		val list = repository.getList(0, setOfNotNull(tag), order).asArrayList()
		if (appSettings.isSuggestionsExcludeNsfw) {
			list.removeAll { it.isNsfw }
		}
		if (blacklist.isNotEmpty()) {
			list.removeAll { manga -> manga in blacklist }
		}
		list
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(emptyList())

	private suspend fun showNotification(manga: Manga) {
		val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				MANGA_CHANNEL_ID,
				applicationContext.getString(R.string.suggestions),
				NotificationManager.IMPORTANCE_DEFAULT,
			)
			channel.description = applicationContext.getString(R.string.suggestions_summary)
			channel.enableLights(true)
			channel.setShowBadge(true)
			manager.createNotificationChannel(channel)
		}
		val id = manga.url.hashCode()
		val title = applicationContext.getString(R.string.suggestion_manga, manga.title)
		val builder = NotificationCompat.Builder(applicationContext, MANGA_CHANNEL_ID)
		val tagsText = manga.tags.joinToString(", ") { it.title }
		with(builder) {
			setContentText(tagsText)
			setContentTitle(title)
			setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.tag(manga.source)
						.build(),
				).toBitmapOrNull(),
			)
			setSmallIcon(R.drawable.ic_stat_suggestion)
			val description = manga.description?.parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT)
			if (!description.isNullOrBlank()) {
				val style = NotificationCompat.BigTextStyle()
				style.bigText(
					buildSpannedString {
						append(tagsText)
						appendLine()
						append(description)
					},
				)
				style.setBigContentTitle(title)
				setStyle(style)
			}
			val intent = DetailsActivity.newIntent(applicationContext, manga)
			setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					id,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT,
					false,
				),
			)
			setAutoCancel(true)
			setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
			setVisibility(if (manga.isNsfw) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC)
			setShortcutId(manga.id.toString())
			priority = NotificationCompat.PRIORITY_DEFAULT

			addAction(
				R.drawable.ic_read,
				applicationContext.getString(R.string.read),
				PendingIntentCompat.getActivity(
					applicationContext,
					id + 2,
					ReaderActivity.newIntent(applicationContext, manga),
					0,
					false,
				),
			)

			addAction(
				R.drawable.ic_suggestion,
				applicationContext.getString(R.string.more),
				PendingIntentCompat.getActivity(
					applicationContext,
					0,
					SuggestionsActivity.newIntent(applicationContext),
					0,
					false,
				),
			)
		}
		manager.notify(TAG, id, builder.build())
	}

	@FloatRange(from = 0.0, to = 1.0)
	private fun computeRelevance(mangaTags: Set<MangaTag>, allTags: List<String>): Float {
		val maxWeight = (allTags.size + allTags.size + 1 - mangaTags.size) * mangaTags.size / 2.0
		val weight = mangaTags.sumOf { tag ->
			val index = allTags.inexactIndexOf(tag.title, TAG_EQ_THRESHOLD)
			if (index < 0) 0 else allTags.size - index
		}
		return (weight / maxWeight).pow(2.0).toFloat()
	}

	private fun Iterable<String>.inexactIndexOf(element: String, threshold: Float): Int {
		forEachIndexed { i, t ->
			if (t.almostEquals(element, threshold)) {
				return i
			}
		}
		return -1
	}

	companion object {

		private const val TAG = "suggestions"
		private const val TAG_ONESHOT = "suggestions_oneshot"
		private const val DATA_COUNT = "count"
		private const val WORKER_CHANNEL_ID = "suggestion_worker"
		private const val MANGA_CHANNEL_ID = "suggestions"
		private const val WORKER_NOTIFICATION_ID = 36
		private const val MAX_RESULTS = 80
		private const val MAX_RAW_RESULTS = 200
		private const val TAG_EQ_THRESHOLD = 0.4f

		private val preferredSortOrders = listOf(
			SortOrder.UPDATED,
			SortOrder.NEWEST,
			SortOrder.POPULARITY,
			SortOrder.RATING,
		)

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
