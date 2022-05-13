package org.koitharu.kotatsu.favourites.ui.categories.edit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.SingleLiveEvent

private const val NO_ID = -1L

class FavouritesCategoryEditViewModel(
	private val categoryId: Long,
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	val onSaved = SingleLiveEvent<Unit>()
	val category = MutableLiveData<FavouriteCategory?>()

	val isTrackerEnabled = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
		emit(settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources)
	}

	init {
		launchLoadingJob {
			category.value = if (categoryId != NO_ID) {
				repository.getCategory(categoryId)
			} else {
				null
			}
		}
	}

	fun save(
		title: String,
		sortOrder: SortOrder,
		isTrackerEnabled: Boolean,
	) {
		launchLoadingJob {
			if (categoryId == NO_ID) {
				repository.createCategory(title, sortOrder, isTrackerEnabled)
			} else {
				repository.updateCategory(categoryId, title, sortOrder, isTrackerEnabled)
			}
			onSaved.call(Unit)
		}
	}
}