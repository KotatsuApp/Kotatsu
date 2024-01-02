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
import org.koitharu.kotatsu.filter.ui.MangaFilter
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.filter.ui.model.TagCatalogItem
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.MangaTag

@HiltViewModel(assistedFactory = TagsCatalogViewModel.Factory::class)
class TagsCatalogViewModel @AssistedInject constructor(
	@Assisted private val filter: MangaFilter,
	@Assisted private val isExcluded: Boolean,
) : BaseViewModel() {

	val searchQuery = MutableStateFlow("")

	private val filterProperty: StateFlow<FilterProperty<MangaTag>>
		get() = if (isExcluded) filter.filterTagsExcluded else filter.filterTags

	private val tags = combine(
		filter.allTags,
		filterProperty.map { it.selectedItems },
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

	fun handleTagClick(tag: MangaTag, isChecked: Boolean) {
		if (isExcluded) {
			filter.setTagExcluded(tag, !isChecked)
		} else {
			filter.setTag(tag, !isChecked)
		}
	}

	@AssistedFactory
	interface Factory {
		fun create(filter: MangaFilter, isExcludeTag: Boolean): TagsCatalogViewModel
	}

}
