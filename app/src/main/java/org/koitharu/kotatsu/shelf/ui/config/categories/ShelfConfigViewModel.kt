package org.koitharu.kotatsu.shelf.ui.config.categories

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.shelf.domain.ShelfSection
import org.koitharu.kotatsu.utils.asFlowLiveData
import javax.inject.Inject

@HiltViewModel
class ShelfConfigViewModel @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	val content = combine(
		settings.observeAsFlow(AppSettings.KEY_SHELF_SECTIONS) { shelfSections },
		favouritesRepository.observeCategories(),
	) { sections, categories ->
		buildList(sections, categories)
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	private var updateJob: Job? = null

	fun toggleItem(item: ShelfConfigModel) {
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.Default) {
			prevJob?.join()
			when (item) {
				is ShelfConfigModel.FavouriteCategory -> {
					favouritesRepository.updateCategory(item.id, !item.isChecked)
				}

				is ShelfConfigModel.Section -> {
					if (item.isChecked) {
						settings.shelfSections -= item.section
					} else {
						settings.shelfSections += item.section
					}
				}
			}
		}
	}

	private fun buildList(sections: Set<ShelfSection>, categories: List<FavouriteCategory>): List<ShelfConfigModel> {
		val result = ArrayList<ShelfConfigModel>()
		for (section in ShelfSection.values()) {
			val isEnabled = section in sections
			result.add(ShelfConfigModel.Section(section, isEnabled))
			if (section == ShelfSection.FAVORITES && isEnabled) {
				categories.mapTo(result) {
					ShelfConfigModel.FavouriteCategory(
						id = it.id,
						title = it.title,
						isChecked = it.isVisibleInLibrary,
					)
				}
			}
		}
		return result
	}
}
