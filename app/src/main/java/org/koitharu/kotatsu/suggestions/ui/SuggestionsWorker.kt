package org.koitharu.kotatsu.suggestions.ui

import android.content.Context
import androidx.work.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.suggestions.domain.MangaSuggestion
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.utils.ext.mangaRepositoryOf
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class SuggestionsWorker(appContext: Context, params: WorkerParameters) :
	CoroutineWorker(appContext, params), KoinComponent {

	private val suggestionRepository by inject<SuggestionRepository>()
	private val historyRepository by inject<HistoryRepository>()

	override suspend fun doWork(): Result {
		val rawResults = ArrayList<Manga>()
		val allTags = historyRepository.getAllTags()
		val tagsBySources = allTags.groupBy { x -> x.source }
		for ((source, tags) in tagsBySources) {
			val repo = mangaRepositoryOf(source)
			tags.flatMapTo(rawResults) { tag ->
				repo.getList(
					offset = 0,
					sortOrder = SortOrder.UPDATED,
					tag = tag,
				)
			}
		}
		suggestionRepository.replace(
			rawResults.distinctBy { manga ->
				manga.id
			}.map { manga ->
				val jointTags = manga.tags intersect allTags
				MangaSuggestion(
					manga = manga,
					relevance = (jointTags.size / manga.tags.size.toDouble()).pow(2.0).toFloat(),
				)
			}
		)
		return Result.success()
	}

	companion object {

		private const val TAG = "suggestions"

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
	}
}