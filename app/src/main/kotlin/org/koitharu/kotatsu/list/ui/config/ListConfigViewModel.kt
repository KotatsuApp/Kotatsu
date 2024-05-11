package org.koitharu.kotatsu.list.ui.config

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.core.util.ext.sortedByOrdinal
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment.Companion.NO_ID
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

@HiltViewModel
class ListConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val settings: AppSettings,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val section = savedStateHandle.require<ListConfigSection>(ListConfigBottomSheet.ARG_SECTION)

	var listMode: ListMode
		get() = when (section) {
			is ListConfigSection.Favorites -> settings.favoritesListMode
			ListConfigSection.History -> settings.historyListMode
			ListConfigSection.Suggestions -> settings.suggestionsListMode
			ListConfigSection.General,
			ListConfigSection.Updated -> settings.listMode
		}
		set(value) {
			when (section) {
				is ListConfigSection.Favorites -> settings.favoritesListMode = value
				ListConfigSection.History -> settings.historyListMode = value
				ListConfigSection.Suggestions -> settings.suggestionsListMode = value
				ListConfigSection.Updated,
				ListConfigSection.General -> settings.listMode = value
			}
		}

	var gridSize: Int
		get() = settings.gridSize
		set(value) {
			settings.gridSize = value
		}

	val isGroupingSupported: Boolean
		get() = section == ListConfigSection.History || section == ListConfigSection.Updated

	val isGroupingAvailable: Boolean
		get() = when (section) {
			ListConfigSection.History -> settings.historySortOrder.isGroupingSupported()
			ListConfigSection.Updated -> true
			else -> false
		}

	var isGroupingEnabled: Boolean
		get() = when (section) {
			ListConfigSection.History -> settings.isHistoryGroupingEnabled
			ListConfigSection.Updated -> settings.isUpdatedGroupingEnabled
			else -> false
		}
		set(value) = when (section) {
			ListConfigSection.History -> settings.isHistoryGroupingEnabled = value
			ListConfigSection.Updated -> settings.isUpdatedGroupingEnabled = value
			else -> Unit
		}

	fun getSortOrders(): List<ListSortOrder>? = when (section) {
		is ListConfigSection.Favorites -> ListSortOrder.FAVORITES
		ListConfigSection.General -> null
		ListConfigSection.History -> ListSortOrder.HISTORY
		ListConfigSection.Suggestions -> ListSortOrder.SUGGESTIONS
		ListConfigSection.Updated -> null
	}?.sortedByOrdinal()

	fun getSelectedSortOrder(): ListSortOrder? = when (section) {
		is ListConfigSection.Favorites -> getCategorySortOrder(section.categoryId)
		ListConfigSection.General -> null
		ListConfigSection.Updated -> null
		ListConfigSection.History -> settings.historySortOrder
		ListConfigSection.Suggestions -> ListSortOrder.RELEVANCE
	}

	fun setSortOrder(position: Int) {
		val value = getSortOrders()?.getOrNull(position) ?: return
		when (section) {
			is ListConfigSection.Favorites -> launchJob {
				if (section.categoryId == NO_ID) {
					settings.allFavoritesSortOrder = value
				} else {
					favouritesRepository.setCategoryOrder(section.categoryId, value)
				}
			}

			ListConfigSection.General -> Unit
			ListConfigSection.History -> settings.historySortOrder = value

			ListConfigSection.Suggestions -> Unit
			ListConfigSection.Updated -> Unit
		}
	}

	private fun getCategorySortOrder(id: Long): ListSortOrder = if (id == NO_ID) {
		settings.allFavoritesSortOrder
	} else runBlocking {
		runCatchingCancellable {
			favouritesRepository.getCategory(id).order
		}.getOrElse {
			settings.allFavoritesSortOrder
		}
	}
}
