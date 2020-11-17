package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.MutableLiveData
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.Manga

abstract class MangaListViewModel : BaseViewModel() {

	val content = MutableLiveData<List<Manga>>()
	val filter = MutableLiveData<MangaFilterConfig>()
}