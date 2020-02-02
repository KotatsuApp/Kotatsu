package org.koitharu.kotatsu.ui.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.domain.HistoryRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class ReaderPresenter : BasePresenter<ReaderView>() {

	fun loadChapter(chapter: MangaChapter) {
		launch {
			viewState.onLoadingStateChanged(isLoading = true)
			try {
				val pages = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(chapter.source).getPages(chapter)
				}
				viewState.onPagesReady(pages)
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

	fun addToHistory(manga: Manga, chapterId: Long, page: Int) {
		launch(Dispatchers.IO) {
			HistoryRepository().use {
				it.addOrUpdate(manga, chapterId, page)
			}
		}
	}

}