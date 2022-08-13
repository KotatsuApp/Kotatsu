package org.koitharu.kotatsu.scrobbling.ui.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView.NO_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.requireValue

class ScrobblingSelectorViewModel @AssistedInject constructor(
	@Assisted val manga: Manga,
	scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
) : BaseViewModel() {

	val availableScrobblers = scrobblers.filter { it.isAvailable }

	val selectedScrobblerIndex = MutableLiveData(0)

	private val scrobblerMangaList = MutableStateFlow<List<ScrobblerManga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private var loadingJob: Job? = null
	private var doneJob: Job? = null
	private var initJob: Job? = null

	private val currentScrobbler: Scrobbler
		get() = availableScrobblers[selectedScrobblerIndex.requireValue()]

	val content: LiveData<List<ListModel>> = combine(
		scrobblerMangaList.filterNotNull(),
		hasNextPage,
	) { list, isHasNextPage ->
		when {
			list.isEmpty() -> listOf()
			isHasNextPage -> list + LoadingFooter
			else -> list
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	val selectedItemId = MutableLiveData(NO_ID)
	val searchQuery = MutableLiveData(manga.title)
	val onClose = SingleLiveEvent<Unit>()

	val isEmpty: Boolean
		get() = scrobblerMangaList.value.isNullOrEmpty()

	init {
		initialize()
	}

	fun search(query: String) {
		loadingJob?.cancel()
		searchQuery.value = query
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
			val offset = if (append) scrobblerMangaList.value?.size ?: 0 else 0
			val list = currentScrobbler.findManga(checkNotNull(searchQuery.value), offset)
			if (!append) {
				scrobblerMangaList.value = list
			} else if (list.isNotEmpty()) {
				scrobblerMangaList.value = scrobblerMangaList.value?.plus(list) ?: list
			}
			hasNextPage.value = list.isNotEmpty()
		}
	}

	fun onDoneClick() {
		if (doneJob?.isActive == true) {
			return
		}
		val targetId = selectedItemId.value ?: NO_ID
		if (targetId == NO_ID) {
			onClose.call(Unit)
		}
		doneJob = launchJob(Dispatchers.Default) {
			currentScrobbler.linkManga(manga.id, targetId)
			onClose.postCall(Unit)
		}
	}

	fun setScrobblerIndex(index: Int) {
		if (index == selectedScrobblerIndex.value || index !in availableScrobblers.indices) return
		selectedScrobblerIndex.value = index
		initialize()
	}

	private fun initialize() {
		initJob?.cancel()
		loadingJob?.cancel()
		hasNextPage.value = false
		scrobblerMangaList.value = null
		initJob = launchJob(Dispatchers.Default) {
			try {
				val info = currentScrobbler.getScrobblingInfoOrNull(manga.id)
				if (info != null) {
					selectedItemId.postValue(info.targetId)
				}
			} finally {
				loadList(append = false)
			}
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(manga: Manga): ScrobblingSelectorViewModel
	}
}
