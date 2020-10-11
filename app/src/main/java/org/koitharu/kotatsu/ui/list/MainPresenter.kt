package org.koitharu.kotatsu.ui.list

import moxy.InjectViewState
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.reader.ReaderState

@InjectViewState
class MainPresenter : BasePresenter<MainView>() {

	fun openLastReader() {
		launchLoadingJob {
			val historyRepository = HistoryRepository()
			val manga = historyRepository.getList(0, 1).firstOrNull()
				?: throw EmptyHistoryException()
			val history = historyRepository.getOne(manga) ?: throw EmptyHistoryException()
			val state = ReaderState(
				MangaProviderFactory.create(manga.source).getDetails(manga),
				history.chapterId, history.page, history.scroll
			)
			viewState.onOpenReader(state)
		}
	}
}