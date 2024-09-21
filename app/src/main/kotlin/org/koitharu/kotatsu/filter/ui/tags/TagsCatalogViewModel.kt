package org.koitharu.kotatsu.filter.ui.tags

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.filter.ui.model.TagCatalogItem
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.parsers.model.MangaTag

@HiltViewModel(assistedFactory = TagsCatalogViewModel.Factory::class)
class TagsCatalogViewModel @AssistedInject constructor(
	@Assisted private val filter: FilterCoordinator,
	@Assisted private val isExcluded: Boolean,
) : BaseViewModel() {

	val searchQuery = MutableStateFlow("")

	private val filterProperty: StateFlow<FilterProperty<MangaTag>>
		get() = if (isExcluded) filter.tagsExcluded else filter.tags

	private val tags: StateFlow<List<ListModel>> = combine(
		filter.getAllTags(),
		filterProperty.map { it.selectedItems },
	) { all, selected ->
		all.fold(
			onSuccess = {
				it.map { tag ->
					TagCatalogItem(
						tag = tag,
						isChecked = tag in selected,
					)
				}
			},
			onFailure = {
				listOf(it.toErrorState(false))
			},
		)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	val content = combine(tags, searchQuery) { raw, query ->
		raw.filter { x ->
			x !is TagCatalogItem || x.tag.title.contains(query, ignoreCase = true)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingState))

	fun handleTagClick(tag: MangaTag, isChecked: Boolean) {
		if (isExcluded) {
			filter.toggleTagExclude(tag, !isChecked)
		} else {
			filter.toggleTag(tag, !isChecked)
		}
	}

	@AssistedFactory
	interface Factory {
		fun create(filter: FilterCoordinator, isExcludeTag: Boolean): TagsCatalogViewModel
	}

}
