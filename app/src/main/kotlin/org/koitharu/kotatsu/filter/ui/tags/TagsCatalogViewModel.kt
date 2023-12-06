package org.koitharu.kotatsu.filter.ui.tags

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.filter.ui.MangaFilter
import org.koitharu.kotatsu.filter.ui.model.TagCatalogItem
import org.koitharu.kotatsu.list.ui.model.LoadingState

@HiltViewModel(assistedFactory = TagsCatalogViewModel.Factory::class)
class TagsCatalogViewModel @AssistedInject constructor(
	@Assisted filter: MangaFilter,
	mangaRepositoryFactory: MangaRepository.Factory,
	dataRepository: MangaDataRepository,
) : BaseViewModel() {

	val searchQuery = MutableStateFlow("")

	private val tags = combine(
		filter.allTags,
		filter.filterTags.map { it.selectedItems },
	) { all, selected ->
		all.map { x ->
			if (x is TagCatalogItem) {
				val checked = x.tag in selected
				if (x.isChecked == checked) {
					x
				} else {
					x.copy(isChecked = checked)
				}
			} else {
				x
			}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, filter.allTags.value)

	val content = combine(tags, searchQuery) { raw, query ->
		raw.filter { x ->
			x !is TagCatalogItem || x.tag.title.contains(query, ignoreCase = true)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingState))

	@AssistedFactory
	interface Factory {
		fun create(filter: MangaFilter): TagsCatalogViewModel
	}

}
