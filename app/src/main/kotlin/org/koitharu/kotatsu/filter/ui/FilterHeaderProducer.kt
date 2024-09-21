package org.koitharu.kotatsu.filter.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import java.util.LinkedList
import javax.inject.Inject

class FilterHeaderProducer @Inject constructor(
	private val searchRepository: MangaSearchRepository,
) {

	fun observeHeader(filterCoordinator: FilterCoordinator): Flow<FilterHeaderModel> {
		return filterCoordinator.tags.mapLatest {
			createChipsList(
				source = filterCoordinator.mangaSource,
				property = it,
				limit = 8,
			)
		}.combine(filterCoordinator.observe()) { chipList, snapshot ->
			FilterHeaderModel(
				chips = chipList,
				sortOrder = snapshot.sortOrder,
				isFilterApplied = !snapshot.listFilter.isEmpty(),
			)
		}
	}

	private suspend fun createChipsList(
		source: MangaSource,
		property: FilterProperty<MangaTag>,
		limit: Int,
	): List<ChipsView.ChipModel> {
		val selectedTags = property.selectedItems.toMutableSet()
		var tags = if (selectedTags.isEmpty()) {
			searchRepository.getTagsSuggestion("", limit, source)
		} else {
			searchRepository.getTagsSuggestion(selectedTags).take(limit)
		}
		if (tags.size < limit) {
			tags = tags + property.availableItems.take(limit - tags.size)
		}
		if (tags.isEmpty() && selectedTags.isEmpty()) {
			return emptyList()
		}
		val result = LinkedList<ChipsView.ChipModel>()
		for (tag in tags) {
			val model = ChipsView.ChipModel(
				title = tag.title,
				isChecked = selectedTags.remove(tag),
				data = tag,
			)
			if (model.isChecked) {
				result.addFirst(model)
			} else {
				result.addLast(model)
			}
		}
		for (tag in selectedTags) {
			val model = ChipsView.ChipModel(
				title = tag.title,
				isChecked = true,
				data = tag,
			)
			result.addFirst(model)
		}
		return result
	}
}
