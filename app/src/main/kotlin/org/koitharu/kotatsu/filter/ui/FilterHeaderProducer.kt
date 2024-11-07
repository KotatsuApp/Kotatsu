package org.koitharu.kotatsu.filter.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.titleResId
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import javax.inject.Inject
import com.google.android.material.R as materialR

class FilterHeaderProducer @Inject constructor(
	private val searchRepository: MangaSearchRepository,
) {

	fun observeHeader(filterCoordinator: FilterCoordinator): Flow<FilterHeaderModel> {
		return combine(filterCoordinator.tags, filterCoordinator.observe()) { tags, snapshot ->
			val chipList = createChipsList(
				source = filterCoordinator.mangaSource,
				capabilities = filterCoordinator.capabilities,
				tagsProperty = tags,
				snapshot = snapshot.listFilter,
				limit = 8,
			)
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
		tagsProperty: FilterProperty<MangaTag>,
		snapshot: MangaListFilter,
		limit: Int,
	): List<ChipsView.ChipModel> {
		val result = ArrayDeque<ChipsView.ChipModel>(limit + 3)
		if (snapshot.query.isNullOrEmpty() || capabilities.isSearchWithFiltersSupported) {
			val selectedTags = tagsProperty.selectedItems.toMutableSet()
			var tags = if (selectedTags.isEmpty()) {
				searchRepository.getTagsSuggestion("", limit, source)
			} else {
				searchRepository.getTagsSuggestion(selectedTags).take(limit)
			}
			if (tags.size < limit) {
				tags = tags + tagsProperty.availableItems.take(limit - tags.size)
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
		snapshot.locale?.let {
			result.addFirst(
				ChipsView.ChipModel(
					title = it.getDisplayName(it).toTitleCase(it),
					icon = R.drawable.ic_language,
					isCloseable = true,
					data = it,
				),
			)
		}
		snapshot.types.forEach {
			result.addFirst(
				ChipsView.ChipModel(
					titleResId = it.titleResId,
					isCloseable = true,
					data = it,
				),
			)
		}
		snapshot.demographics.forEach {
			result.addFirst(
				ChipsView.ChipModel(
					titleResId = it.titleResId,
					isCloseable = true,
					data = it,
				),
			)
		}
		snapshot.contentRating.forEach {
			result.addFirst(
				ChipsView.ChipModel(
					titleResId = it.titleResId,
					isCloseable = true,
					data = it,
				),
			)
		}
		snapshot.states.forEach {
			result.addFirst(
				ChipsView.ChipModel(
					titleResId = it.titleResId,
					isCloseable = true,
					data = it,
				),
			)
		}
		if (!snapshot.query.isNullOrEmpty()) {
			result.addFirst(
				ChipsView.ChipModel(
					title = snapshot.query,
					icon = materialR.drawable.abc_ic_search_api_material,
					isCloseable = true,
					data = snapshot.query,
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
	)
}
