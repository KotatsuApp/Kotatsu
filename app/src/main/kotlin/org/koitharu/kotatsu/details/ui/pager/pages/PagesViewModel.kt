package org.koitharu.kotatsu.details.ui.pager.pages

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.firstNotNull
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail
import javax.inject.Inject

@HiltViewModel
class PagesViewModel @Inject constructor(
	private val chaptersLoader: ChaptersLoader,
) : BaseViewModel() {

	private var loadingJob: Job? = null
	private var loadingPrevJob: Job? = null
	private var loadingNextJob: Job? = null

	private val state = MutableStateFlow<State?>(null)
	val thumbnails = MutableStateFlow<List<ListModel>>(emptyList())
	val isLoadingUp = MutableStateFlow(false)
	val isLoadingDown = MutableStateFlow(false)

	init {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val firstState = state.firstNotNull()
			doInit(firstState)
			launchJob(Dispatchers.Default) {
				state.collectLatest {
					if (it != null) {
						doInit(it)
					}
				}
			}
		}
	}

	fun updateState(newState: State?) {
		if (newState != null) {
			state.value = newState
		}
	}

	fun loadPrevChapter() {
		if (loadingJob?.isActive == true || loadingPrevJob?.isActive == true) {
			return
		}
		loadingPrevJob = loadPrevNextChapter(isNext = false)
	}

	fun loadNextChapter() {
		if (loadingJob?.isActive == true || loadingNextJob?.isActive == true) {
			return
		}
		loadingNextJob = loadPrevNextChapter(isNext = true)
	}

	private suspend fun doInit(state: State) {
		chaptersLoader.init(state.details)
		val initialChapterId = state.history?.chapterId ?: state.details.allChapters.firstOrNull()?.id ?: return
		if (!chaptersLoader.hasPages(initialChapterId)) {
			chaptersLoader.loadSingleChapter(initialChapterId)
		}
		updateList(state.history)
	}

	private fun loadPrevNextChapter(isNext: Boolean): Job = launchJob(Dispatchers.Default) {
		val indicator = if (isNext) isLoadingDown else isLoadingUp
		indicator.value = true
		try {
			val currentState = state.firstNotNull()
			val currentId = (if (isNext) chaptersLoader.last() else chaptersLoader.first()).chapterId
			chaptersLoader.loadPrevNextChapter(currentState.details, currentId, isNext)
			updateList(currentState.history)
		} finally {
			indicator.value = false
		}
	}

	private fun updateList(history: MangaHistory?) {
		val snapshot = chaptersLoader.snapshot()
		val pages = buildList(snapshot.size + chaptersLoader.size + 2) {
			var previousChapterId = 0L
			for (page in snapshot) {
				if (page.chapterId != previousChapterId) {
					chaptersLoader.peekChapter(page.chapterId)?.let {
						add(ListHeader(it.name))
					}
					previousChapterId = page.chapterId
				}
				this += PageThumbnail(
					isCurrent = history?.let {
						page.chapterId == it.chapterId && page.index == it.page
					} ?: false,
					page = page,
				)
			}
		}
		thumbnails.value = pages
	}

	data class State(
		val details: MangaDetails,
		val history: MangaHistory?,
		val branch: String?
	)
}
