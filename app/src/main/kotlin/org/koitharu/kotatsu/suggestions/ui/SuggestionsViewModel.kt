package org.koitharu.kotatsu.suggestions.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.onFirst
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
	repository: SuggestionRepository,
	settings: AppSettings,
	private val tagHighlighter: MangaTagHighlighter,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	override val content = combine(
		repository.observeAll(),
		listMode,
	) { list, mode ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_common,
					textPrimary = R.string.nothing_found,
					textSecondary = R.string.text_suggestion_holder,
					actionStringRes = 0,
				),
			)

			else -> list.toUi(mode, tagHighlighter)
		}
	}.onStart {
		loadingCounter.increment()
	}.onFirst {
		loadingCounter.decrement()
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit
}
