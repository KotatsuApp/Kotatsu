package org.koitharu.kotatsu.suggestions.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.onFirst

class SuggestionsViewModel(
	repository: SuggestionRepository,
	settings: AppSettings,
) : MangaListViewModel(settings) {

	private val headerModel = ListHeader(null, R.string.suggestions)

	override val content = combine(
		repository.observeAll(),
		createListModeFlow()
	) { list, mode ->
		when {
			list.isEmpty() -> listOf(EmptyState(
				icon = R.drawable.ic_book_cross,
				textPrimary = R.string.nothing_found,
				textSecondary = R.string.text_suggestion_holder,
			))
			else -> buildList<ListModel>(list.size + 1) {
				add(headerModel)
				list.toUi(this, mode)
			}
		}
	}.onFirst {
		isLoading.postValue(false)
	}.catch {
		it.toErrorState(canRetry = false)
	}.asLiveDataDistinct(
		viewModelScope.coroutineContext + Dispatchers.Default,
		listOf(LoadingState)
	)

	override fun onRefresh() = Unit

	override fun onRetry() = Unit
}