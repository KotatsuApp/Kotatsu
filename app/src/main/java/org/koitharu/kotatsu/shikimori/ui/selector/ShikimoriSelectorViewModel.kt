package org.koitharu.kotatsu.shikimori.ui.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.shikimori.data.ShikimoriRepository
import org.koitharu.kotatsu.shikimori.data.model.ShikimoriManga
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class ShikimoriSelectorViewModel(
	val manga: Manga,
	private val repository: ShikimoriRepository,
) : BaseViewModel() {

	private val shikiMangaList = MutableStateFlow<List<ShikimoriManga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private var loadingJob: Job? = null

	val content: LiveData<List<ListModel>> = combine(
		shikiMangaList.filterNotNull(),
		hasNextPage
	) { list, isHasNextPage ->
		when {
			list.isEmpty() -> listOf()
			isHasNextPage -> list + LoadingFooter
			else -> list
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	val selectedItemId = MutableLiveData(RecyclerView.NO_ID)

	val avatar = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
		emit(repository.getCachedUser()?.avatar)
		emit(runCatching { repository.getUser().avatar }.getOrNull())
	}

	val isEmpty: Boolean
		get() = shikiMangaList.value.isNullOrEmpty()

	init {
		loadList(append = false)
	}

	fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		if (append && !hasNextPage.value) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val offset = if (append) shikiMangaList.value?.size ?: 0 else 0
			val list = repository.findManga(manga.title, offset)
			if (!append) {
				shikiMangaList.value = list
			} else if (list.isNotEmpty()) {
				shikiMangaList.value = shikiMangaList.value?.plus(list) ?: list
			}
			hasNextPage.value = list.isNotEmpty()
		}
	}
}