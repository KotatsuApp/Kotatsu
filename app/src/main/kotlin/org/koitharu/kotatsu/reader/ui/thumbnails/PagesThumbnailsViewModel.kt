package org.koitharu.kotatsu.reader.ui.thumbnails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koitharu.kotatsu.core.model.findById
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.firstNotNull
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import javax.inject.Inject

@HiltViewModel
class PagesThumbnailsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val chaptersLoader: ChaptersLoader,
	detailsLoadUseCase: DetailsLoadUseCase,
) : BaseViewModel() {

	private val currentPageIndex: Int =
		savedStateHandle[PagesThumbnailsSheet.ARG_CURRENT_PAGE] ?: -1
	private val initialChapterId: Long = savedStateHandle[PagesThumbnailsSheet.ARG_CHAPTER_ID] ?: 0L
	val manga = savedStateHandle.require<ParcelableManga>(PagesThumbnailsSheet.ARG_MANGA).manga

	private val mangaDetails = detailsLoadUseCase(MangaIntent.of(manga)).map {
		val b = manga.chapters?.findById(initialChapterId)?.branch
		branch.value = b
		it.filterChapters(b)
	}.withErrorHandling()
		.stateIn(viewModelScope, SharingStarted.Lazily, null)
	private var loadingJob: Job
	private var loadingPrevJob: Job? = null
	private var loadingNextJob: Job? = null

	val thumbnails = MutableStateFlow<List<ListModel>>(emptyList())
	val branch = MutableStateFlow<String?>(null)

	init {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			chaptersLoader.init(checkNotNull(mangaDetails.first { x -> x?.isLoaded == true }))
			chaptersLoader.loadSingleChapter(initialChapterId)
			updateList()
		}
	}

	fun loadPrevChapter() {
		if (loadingJob.isActive || loadingPrevJob?.isActive == true) {
			return
		}
		loadingPrevJob = loadPrevNextChapter(isNext = false)
	}

	fun loadNextChapter() {
		if (loadingJob.isActive || loadingNextJob?.isActive == true) {
			return
		}
		loadingNextJob = loadPrevNextChapter(isNext = true)
	}

	private fun loadPrevNextChapter(isNext: Boolean): Job = launchLoadingJob(Dispatchers.Default) {
		val currentId = (if (isNext) chaptersLoader.last() else chaptersLoader.first()).chapterId
		chaptersLoader.loadPrevNextChapter(mangaDetails.firstNotNull(), currentId, isNext)
		updateList()
	}

	private fun updateList() {
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
					isCurrent = page.chapterId == initialChapterId && page.index == currentPageIndex,
					page = page,
				)
			}
		}
		thumbnails.value = pages
	}
}
