package org.koitharu.kotatsu.suggestions.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import androidx.annotation.FloatRange
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
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
import androidx.work.await
import androidx.work.workDataOf
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.cloudflare.CaptchaNotifier
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.distinctById
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.almostEquals
import org.koitharu.kotatsu.core.util.ext.asArrayList
import org.koitharu.kotatsu.core.util.ext.awaitUniqueWorkInfoByName
import org.koitharu.kotatsu.core.util.ext.awaitWorkInfosByTag
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.flatten
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.core.util.ext.sizeOrZero
import org.koitharu.kotatsu.core.util.ext.takeMostFrequent
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.core.util.ext.trySetForeground
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.work.PeriodicWorkScheduler
import org.koitharu.kotatsu.suggestions.domain.MangaSuggestion
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.suggestions.domain.TagsBlacklist
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.pow
import kotlin.random.Random
import com.google.android.material.R as materialR

@HiltWorker
class SuggestionsWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val coil: ImageLoader,
	private val suggestionRepository: SuggestionRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val appSettings: AppSettings,
	private val workManager: WorkManager,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val sourcesRepository: MangaSourcesRepository,
) : CoroutineWorker(appContext, params) {

	private val notificationManager by lazy { NotificationManagerCompat.from(appContext) }

	override suspend fun doWork(): Result {
		trySetForeground()
		if (!appSettings.isSuggestionsEnabled) {
			suggestionRepository.clear()
			return Result.success()
		}
		val count = doWorkImpl()
		val outputData = workDataOf(DATA_COUNT to count)
		return Result.success(outputData)
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.suggestions_updating)
		val channel = NotificationChannelCompat.Builder(WORKER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
			.setName(title)
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(true)
			.build()
		notificationManager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
			.setContentTitle(title)
			.setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					0,
					SettingsActivity.newSuggestionsSettingsIntent(applicationContext),
					0,
					false,
				),
			).addAction(
				materialR.drawable.material_ic_clear_black_24dp,
				applicationContext.getString(android.R.string.cancel),
				workManager.createCancelPendingIntent(id),
			)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setDefaults(0)
			.setOngoing(false)
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(
				if (TAG_ONESHOT in tags) {
					NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
				} else {
					NotificationCompat.FOREGROUND_SERVICE_DEFERRED
				},
			)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val actionIntent = PendingIntentCompat.getActivity(
				applicationContext, SETTINGS_ACTION_CODE,
				Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
					.putExtra(Settings.EXTRA_APP_PACKAGE, applicationContext.packageName)
					.putExtra(Settings.EXTRA_CHANNEL_ID, WORKER_CHANNEL_ID),
				0, false,
			)
			notification.addAction(
				R.drawable.ic_settings,
				applicationContext.getString(R.string.notifications_settings),
				actionIntent,
			)
		}
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			ForegroundInfo(WORKER_NOTIFICATION_ID, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
		} else {
			ForegroundInfo(WORKER_NOTIFICATION_ID, notification.build())
		}
	}

	private suspend fun doWorkImpl(): Int {
		val seed = (
			historyRepository.getList(0, 20) +
				favouritesRepository.getLastManga(20)
			).distinctById()
		val sources = sourcesRepository.getEnabledSources()
		if (seed.isEmpty() || sources.isEmpty()) {
			return 0
		}
		val tagsBlacklist = TagsBlacklist(appSettings.suggestionsTagsBlacklist, TAG_EQ_THRESHOLD)
		val tags = seed.flatMap { it.tags.map { x -> x.title } }.takeMostFrequent(10)

		val semaphore = Semaphore(MAX_PARALLELISM)
		val producer = channelFlow {
			for (it in sources.shuffled()) {
				if (it.isNsfw() && (appSettings.isSuggestionsExcludeNsfw || appSettings.isNsfwContentDisabled)) {
					continue
				}
				launch {
					semaphore.withPermit {
						send(getList(it, tags, tagsBlacklist))
					}
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
		if (appSettings.isSuggestionsNotificationAvailable
			&& applicationContext.checkNotificationPermission(MANGA_CHANNEL_ID)
		) {
			for (i in 0..3) {
				try {
					val manga = suggestions[Random.nextInt(0, suggestions.size / 3)]
					val details = mangaRepositoryFactory.create(manga.manga.source)
						.getDetails(manga.manga)
					if (details.chapters.isNullOrEmpty()) {
						continue
					}
					if (details.rating > 0 && details.rating < RATING_MIN) {
						continue
					}
					if (details.isNsfw && (appSettings.isSuggestionsExcludeNsfw || appSettings.isNsfwContentDisabled)) {
						continue
					}
					if (details in tagsBlacklist) {
						continue
					}
					showNotification(details)
					break
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					e.printStackTraceDebug()
				}
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
		val availableTags = repository.getFilterOptions().availableTags
		val tag = tags.firstNotNullOfOrNull { title ->
			availableTags.find { x -> x !in blacklist && x.title.almostEquals(title, TAG_EQ_THRESHOLD) }
		}
		val list = repository.getList(
			offset = 0,
			order = order,
			filter = MangaListFilter(tags = setOfNotNull(tag)),
		).asArrayList()
		if (appSettings.isSuggestionsExcludeNsfw) {
			list.removeAll { it.isNsfw }
		}
		if (blacklist.isNotEmpty()) {
			list.removeAll { manga -> manga in blacklist }
		}
		list.shuffle()
		list.take(MAX_SOURCE_RESULTS)
	}.onFailure { e ->
		if (e is CloudFlareProtectedException) {
			CaptchaNotifier(applicationContext).notify(e)
		}
		e.printStackTraceDebug()
	}.getOrDefault(emptyList())

	@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
	private suspend fun showNotification(manga: Manga) {
		val channel = NotificationChannelCompat.Builder(MANGA_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
			.setName(applicationContext.getString(R.string.suggestions))
			.setDescription(applicationContext.getString(R.string.suggestions_summary))
			.setLightsEnabled(true)
			.setShowBadge(true)
			.build()
		notificationManager.createNotificationChannel(channel)

		val id = manga.url.hashCode()
		val title = applicationContext.getString(R.string.suggestion_manga, manga.title)
		val builder = NotificationCompat.Builder(applicationContext, MANGA_CHANNEL_ID)
		val tagsText = manga.tags.joinToString(", ") { it.title }
		with(builder) {
			setContentText(tagsText)
			setContentTitle(title)
			setGroup(GROUP_SUGGESTION)
			setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.mangaSourceExtra(manga.source)
						.build(),
				).toBitmapOrNull(),
			)
			setSmallIcon(R.drawable.ic_stat_suggestion)
			val description = manga.description?.parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT)?.sanitize()
			if (!description.isNullOrBlank()) {
				val style = NotificationCompat.BigTextStyle()
				style.bigText(
					buildSpannedString {
						append(tagsText)
						val chaptersCount = manga.chapters.sizeOrZero()
						appendLine()
						bold {
							append(
								applicationContext.resources.getQuantityString(
									R.plurals.chapters,
									chaptersCount,
									chaptersCount,
								),
							)
						}
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
					IntentBuilder(applicationContext).manga(manga).build(),
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
		notificationManager.notify(TAG, id, builder.build())
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

	@Reusable
	class Scheduler @Inject constructor(
		private val workManager: WorkManager,
		private val settings: AppSettings,
	) : PeriodicWorkScheduler {

		override suspend fun schedule() {
			val request = PeriodicWorkRequestBuilder<SuggestionsWorker>(6, TimeUnit.HOURS)
				.setConstraints(createConstraints())
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
				.build()
			workManager
				.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
				.await()
		}

		override suspend fun unschedule() {
			workManager
				.cancelUniqueWork(TAG)
				.await()
		}

		override suspend fun isScheduled(): Boolean {
			return workManager
				.awaitUniqueWorkInfoByName(TAG)
				.any { !it.state.isFinished }
		}

		suspend fun startNow() {
			if (workManager.awaitWorkInfosByTag(TAG_ONESHOT).any { !it.state.isFinished }) {
				return
			}
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			val request = OneTimeWorkRequestBuilder<SuggestionsWorker>()
				.setConstraints(constraints)
				.addTag(TAG_ONESHOT)
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
				.build()
			workManager.enqueue(request).await()
		}

		private fun createConstraints() = Constraints.Builder()
			.setRequiredNetworkType(if (settings.isSuggestionsWiFiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
			.setRequiresBatteryNotLow(true)
			.build()
	}

	private companion object {

		const val TAG = "suggestions"
		const val TAG_ONESHOT = "suggestions_oneshot"
		const val DATA_COUNT = "count"
		const val WORKER_CHANNEL_ID = "suggestion_worker"
		const val MANGA_CHANNEL_ID = "suggestions"
		const val GROUP_SUGGESTION = "org.koitharu.kotatsu.SUGGESTIONS"
		const val WORKER_NOTIFICATION_ID = 36
		const val MAX_RESULTS = 160
		const val MAX_PARALLELISM = 3
		const val MAX_SOURCE_RESULTS = 20
		const val MAX_RAW_RESULTS = 280
		const val TAG_EQ_THRESHOLD = 0.4f
		const val RATING_MIN = 0.5f
		const val SETTINGS_ACTION_CODE = 4

		val preferredSortOrders = listOf(
			SortOrder.UPDATED,
			SortOrder.NEWEST,
			SortOrder.POPULARITY,
			SortOrder.RATING,
		)
	}
}
