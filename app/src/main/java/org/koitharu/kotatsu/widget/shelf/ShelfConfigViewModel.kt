package org.koitharu.kotatsu.widget.shelf

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.widget.shelf.model.CategoryItem
import javax.inject.Inject

@HiltViewModel
class ShelfConfigViewModel @Inject constructor(
	favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	private val selectedCategoryId = MutableStateFlow(0L)

	val content: LiveData<List<CategoryItem>> = combine(
		favouritesRepository.observeCategories(),
		selectedCategoryId,
	) { categories, selectedId ->
		val list = ArrayList<CategoryItem>(categories.size + 1)
		list += CategoryItem(0L, null, selectedId == 0L)
		categories.mapTo(list) {
			CategoryItem(it.id, it.title, selectedId == it.id)
		}
		list
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	var checkedId: Long by selectedCategoryId::value
}
