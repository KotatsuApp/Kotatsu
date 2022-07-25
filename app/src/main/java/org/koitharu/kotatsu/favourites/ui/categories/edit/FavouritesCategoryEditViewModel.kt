package org.koitharu.kotatsu.favourites.ui.categories.edit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.SingleLiveEvent

private const val NO_ID = -1L

class FavouritesCategoryEditViewModel @AssistedInject constructor(
	@Assisted private val categoryId: Long,
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
			check(title.isNotEmpty())
			if (categoryId == NO_ID) {
				repository.createCategory(title, sortOrder, isTrackerEnabled)
			} else {
				repository.updateCategory(categoryId, title, sortOrder, isTrackerEnabled)
			}
			onSaved.call(Unit)
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(categoryId: Long): FavouritesCategoryEditViewModel
	}
}
