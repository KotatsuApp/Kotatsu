package org.koitharu.kotatsu.reader.ui.thumbnails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.emitValue
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.local.domain.DoubleMangaLoader
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import javax.inject.Inject

@HiltViewModel
class PagesThumbnailsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	private val chaptersLoader: ChaptersLoader,
	private val mangaLoader: DoubleMangaLoader,
) : BaseViewModel() {

	private val currentPageIndex: Int = savedStateHandle[PagesThumbnailsSheet.ARG_CURRENT_PAGE] ?: -1
	private val initialChapterId: Long = savedStateHandle[PagesThumbnailsSheet.ARG_CHAPTER_ID] ?: 0L
	val manga = requireNotNull(savedStateHandle.get<ParcelableManga>(PagesThumbnailsSheet.ARG_MANGA)).manga

	private val repository = mangaRepositoryFactory.create(manga.source)
	private val mangaDetails = SuspendLazy {
		mangaLoader.load(manga).let {
			val b = manga.chapters?.find { ch -> ch.id == initialChapterId }?.branch
			branch.emitValue(b)
			it.filterChapters(b)
		}
	}
	private var loadingJob: Job? = null
	private var loadingPrevJob: Job? = null
	private var loadingNextJob: Job? = null

	val thumbnails = MutableLiveData<List<ListModel>>()
	val branch = MutableLiveData<String?>()
	val title = manga.title

	init {
		loadingJob = launchJob(Dispatchers.Default) {
			chaptersLoader.init(mangaDetails.get())
			chaptersLoader.loadSingleChapter(initialChapterId)
			updateList()
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

	private fun loadPrevNextChapter(isNext: Boolean): Job = launchLoadingJob(Dispatchers.Default) {
		val currentId = (if (isNext) chaptersLoader.last() else chaptersLoader.first()).chapterId
		chaptersLoader.loadPrevNextChapter(mangaDetails.get(), currentId, isNext)
		updateList()
	}

	private suspend fun updateList() {
		val snapshot = chaptersLoader.snapshot()
		val mangaChapters = mangaDetails.tryGet().getOrNull()?.chapters.orEmpty()
		val hasPrevChapter = snapshot.firstOrNull()?.chapterId != mangaChapters.firstOrNull()?.id
		val hasNextChapter = snapshot.lastOrNull()?.chapterId != mangaChapters.lastOrNull()?.id
		val pages = buildList(snapshot.size + chaptersLoader.size + 2) {
			if (hasPrevChapter) {
				add(LoadingFooter(-1))
			}
			var previousChapterId = 0L
			for (page in snapshot) {
				if (page.chapterId != previousChapterId) {
					chaptersLoader.peekChapter(page.chapterId)?.let {
						add(ListHeader(it.name, 0, null))
					}
					previousChapterId = page.chapterId
				}
				this += PageThumbnail(
					isCurrent = page.chapterId == initialChapterId && page.index == currentPageIndex,
					repository = repository,
					page = page,
				)
			}
			if (hasNextChapter) {
				add(LoadingFooter(1))
			}
		}
		thumbnails.emitValue(pages)
	}
}
