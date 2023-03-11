package org.koitharu.kotatsu.favourites.ui.categories.edit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity.Companion.EXTRA_ID
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity.Companion.NO_ID
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class FavouritesCategoryEditViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private val categoryId = savedStateHandle[EXTRA_ID] ?: NO_ID

	val onSaved = SingleLiveEvent<Unit>()
	val category = MutableLiveData<FavouriteCategory?>()

	val isTrackerEnabled = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
		emit(settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources)
	}

	init {
		launchLoadingJob(Dispatchers.Default) {
			category.postValue(
				if (categoryId != NO_ID) {
					repository.getCategory(categoryId)
				} else {
					null
				},
			)
		}
	}

	fun save(
		title: String,
		sortOrder: SortOrder,
		isTrackerEnabled: Boolean,
	) {
		launchLoadingJob(Dispatchers.Default) {
			check(title.isNotEmpty())
			if (categoryId == NO_ID) {
				repository.createCategory(title, sortOrder, isTrackerEnabled)
			} else {
				repository.updateCategory(categoryId, title, sortOrder, isTrackerEnabled)
			}
			onSaved.postCall(Unit)
		}
	}
}
