package org.koitharu.kotatsu.ui.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.domain.HistoryRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class ReaderPresenter : BasePresenter<ReaderView>() {

	fun loadChapter(state: ReaderState) {
		launch {
			viewState.onLoadingStateChanged(isLoading = true)
			try {
				val pages = withContext(Dispatchers.IO) {
					val repo = MangaProviderFactory.create(state.manga.source)
					val chapter = state.chapter ?: repo.getDetails(state.manga).chapters
						?.first { it.id == state.chapterId }
						?: throw RuntimeException("Chapter ${state.chapterId} not found")
					repo.getPages(chapter)
				}
				viewState.onPagesReady(pages, state.page)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(isLoading = false)
			}
		}
	}

	fun saveState(state: ReaderState) {
		launch(Dispatchers.IO) {
			HistoryRepository().addOrUpdate(
				manga = state.manga,
				chapterId = state.chapterId,
				page = state.page
			)
		}
	}

}