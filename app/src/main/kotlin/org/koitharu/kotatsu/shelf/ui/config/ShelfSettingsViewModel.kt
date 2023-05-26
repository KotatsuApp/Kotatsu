package org.koitharu.kotatsu.shelf.ui.config

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.asFlowLiveData
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.parsers.util.move
import org.koitharu.kotatsu.shelf.domain.ShelfSection
import javax.inject.Inject

@HiltViewModel
class ShelfSettingsViewModel @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	val content = combine(
		settings.observeAsFlow(AppSettings.KEY_SHELF_SECTIONS) { shelfSections },
		settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled },
		favouritesRepository.observeCategories(),
	) { sections, isTrackerEnabled, categories ->
		buildList(sections, isTrackerEnabled, categories)
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	private var updateJob: Job? = null

	fun setItemChecked(item: ShelfSettingsItemModel, isChecked: Boolean) {
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			when (item) {
				is ShelfSettingsItemModel.FavouriteCategory -> {
					favouritesRepository.updateCategory(item.id, isChecked)
				}

				is ShelfSettingsItemModel.Section -> {
					val sections = settings.shelfSections
					settings.shelfSections = if (isChecked) {
						sections + item.section
					} else {
						if (sections.size > 1) {
							sections - item.section
						} else {
							return@launchJob
						}
					}
				}
			}
		}
	}

	fun reorderSections(oldPos: Int, newPos: Int): Boolean {
		val snapshot = content.value?.toMutableList() ?: return false
		snapshot.move(oldPos, newPos)
		settings.shelfSections = snapshot.sections()
		return true
	}

	private fun buildList(
		sections: List<ShelfSection>,
		isTrackerEnabled: Boolean,
		categories: List<FavouriteCategory>
	): List<ShelfSettingsItemModel> {
		val result = ArrayList<ShelfSettingsItemModel>()
		val sectionsList = ShelfSection.values().toMutableList()
		if (!isTrackerEnabled) {
			sectionsList.remove(ShelfSection.UPDATED)
		}
		for (section in sections) {
			if (sectionsList.remove(section)) {
				result.addSection(section, true, categories)
			}
		}
		for (section in sectionsList) {
			result.addSection(section, false, categories)
		}
		return result
	}

	private fun MutableList<in ShelfSettingsItemModel>.addSection(
		section: ShelfSection,
		isEnabled: Boolean,
		favouriteCategories: List<FavouriteCategory>,
	) {
		add(ShelfSettingsItemModel.Section(section, isEnabled))
		if (isEnabled && section == ShelfSection.FAVORITES) {
			favouriteCategories.mapTo(this) {
				ShelfSettingsItemModel.FavouriteCategory(
					id = it.id,
					title = it.title,
					isChecked = it.isVisibleInLibrary,
				)
			}
		}
	}

	private fun List<ShelfSettingsItemModel>.sections(): List<ShelfSection> {
		return mapNotNull { (it as? ShelfSettingsItemModel.Section)?.takeIf { x -> x.isChecked }?.section }
	}
}
