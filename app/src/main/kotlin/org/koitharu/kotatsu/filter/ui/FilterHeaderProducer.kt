package org.koitharu.kotatsu.filter.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import javax.inject.Inject
import com.google.android.material.R as materialR

class FilterHeaderProducer @Inject constructor(
	private val searchRepository: MangaSearchRepository,
) {

	fun observeHeader(filterCoordinator: FilterCoordinator): Flow<FilterHeaderModel> {
		return combine(filterCoordinator.tags, filterCoordinator.query) { tags, query ->
			createChipsList(
				source = filterCoordinator.mangaSource,
				capabilities = filterCoordinator.capabilities,
				property = tags,
				query = query,
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
		capabilities: MangaListFilterCapabilities,
		property: FilterProperty<MangaTag>,
		query: String?,
		limit: Int,
	): List<ChipsView.ChipModel> {
		val result = ArrayDeque<ChipsView.ChipModel>(limit + 3)
		if (query.isNullOrEmpty() || capabilities.isSearchWithFiltersSupported) {
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
		}
		if (!query.isNullOrEmpty()) {
			result.addFirst(
				ChipsView.ChipModel(
					title = query,
					icon = materialR.drawable.abc_ic_search_api_material,
					isCloseable = true,
					data = query,
				),
			)
		}
		val hasTags = result.any { it.data is MangaTag }
		if (hasTags) {
			result.addLast(moreTagsChip())
		}
		return result
	}

	private fun moreTagsChip() = ChipsView.ChipModel(
		titleResId = R.string.more,
		isDropdown = true,
		// icon = materialR.drawable.abc_ic_menu_overflow_material,
	)
}
