package org.koitharu.kotatsu.suggestions.ui

import android.content.Context
import androidx.work.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.suggestions.domain.MangaSuggestion
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import java.util.concurrent.TimeUnit
import kotlin.math.pow

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
				.build()
			WorkManager.getInstance(context)
				.enqueue(request)
		}
	}
}