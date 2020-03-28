package org.koitharu.kotatsu.ui.main

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.reader.ReaderState

@InjectViewState
class MainPresenter : BasePresenter<MainView>() {

	fun openLastReader() {
		presenterScope.launch {
			viewState.onLoadingStateChanged(isLoading = true)
			try {
				val state = withContext(Dispatchers.IO) {
					val repo = HistoryRepository()
					val manga = repo.getList(0, 1).firstOrNull()
						?: throw EmptyHistoryException()
					val history = repo.getOne(manga) ?: throw EmptyHistoryException()
					ReaderState(
						MangaProviderFactory.create(manga.source).getDetails(manga),
						history.chapterId, history.page, history.scroll
					)
				}
				viewState.onOpenReader(state)
			} catch (_: CancellationException) {
			} catch (e: Throwable) {
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(isLoading = false)
			}
		}
	}
}