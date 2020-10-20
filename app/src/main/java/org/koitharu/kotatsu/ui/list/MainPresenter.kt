package org.koitharu.kotatsu.ui.list

import moxy.InjectViewState
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.reader.ReaderState

@InjectViewState
class MainPresenter : BasePresenter<MainView>() {

	private val historyRepository by inject<HistoryRepository>()

	fun openLastReader() {
		launchLoadingJob {
			val manga = historyRepository.getList(0, 1).firstOrNull()
				?: throw EmptyHistoryException()
			val history = historyRepository.getOne(manga) ?: throw EmptyHistoryException()
			val state = ReaderState(
				manga.source.repository.getDetails(manga),
				history.chapterId, history.page, history.scroll
			)
			viewState.onOpenReader(state)
		}
	}
}