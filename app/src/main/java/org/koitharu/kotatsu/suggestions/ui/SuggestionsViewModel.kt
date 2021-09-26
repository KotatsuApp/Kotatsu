package org.koitharu.kotatsu.suggestions.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.onFirst

class SuggestionsViewModel(
	repository: SuggestionRepository,
	settings: AppSettings,
) : MangaListViewModel(settings) {

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
			else -> mapList(list, mode)
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

	private fun mapList(
		list: List<Manga>,
		mode: ListMode,
	): List<ListModel> = list.map { manga ->
		when (mode) {
			ListMode.LIST -> manga.toListModel()
			ListMode.DETAILED_LIST -> manga.toListDetailedModel()
			ListMode.GRID -> manga.toGridModel()
		}
	}
}