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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.filter.ui.model.TagCatalogItem
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorFooter
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaTag

@HiltViewModel(assistedFactory = TagsCatalogViewModel.Factory::class)
class TagsCatalogViewModel @AssistedInject constructor(
	@Assisted private val filter: FilterCoordinator,
	@Assisted private val isExcluded: Boolean,
	private val mangaDataRepository: MangaDataRepository,
) : BaseViewModel() {

	val searchQuery = MutableStateFlow("")

	private val filterProperty: StateFlow<FilterProperty<MangaTag>>
		get() = if (isExcluded) filter.tagsExcluded else filter.tags

	@Suppress("RemoveExplicitTypeArguments")
	private val tags: StateFlow<List<ListModel>> = combine(
		filter.getAllTags(),
		flow<Collection<MangaTag>> { emit(emptyList()); emit(mangaDataRepository.findTags(filter.mangaSource)) },
		filterProperty.map { it.selectedItems },
	) { available, cached, selected ->
		buildList(available, cached, selected)
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

	private fun buildList(
		available: Result<List<MangaTag>>,
		cached: Collection<MangaTag>,
		selected: Set<MangaTag>,
	): List<ListModel> {
		val capacity = (available.getOrNull()?.size ?: 1) + cached.size
		val result = ArrayList<ListModel>(capacity)
		val added = HashSet<String>(capacity)
		available.getOrNull()?.forEach { tag ->
			if (added.add(tag.title)) {
				result.add(
					TagCatalogItem(
						tag = tag,
						isChecked = tag in selected,
					),
				)
			}
		}
		cached.forEach { tag ->
			if (added.add(tag.title)) {
				result.add(
					TagCatalogItem(
						tag = tag,
						isChecked = tag in selected,
					),
				)
			}
		}
		if (result.isNotEmpty()) {
			val locale = (filter.mangaSource as? MangaParserSource)?.locale
			result.sortWith(compareBy(TagTitleComparator(locale)) { (it as TagCatalogItem).tag })
		}
		available.exceptionOrNull()?.let { error ->
			result.add(
				if (result.isEmpty()) {
					error.toErrorState(canRetry = false)
				} else {
					error.toErrorFooter()
				},
			)
		}
		return result
	}

	@AssistedFactory
	interface Factory {
		fun create(filter: FilterCoordinator, isExcludeTag: Boolean): TagsCatalogViewModel
	}

}
