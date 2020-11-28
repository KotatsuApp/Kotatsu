package org.koitharu.kotatsu.main.ui

import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.SingleLiveEvent

class MainViewModel(
	private val historyRepository: HistoryRepository
) : BaseViewModel() {

	val onOpenReader = SingleLiveEvent<ReaderState>()

	fun openLastReader() {
		launchLoadingJob {
			val manga = historyRepository.getList(0, 1).firstOrNull()
				?: throw EmptyHistoryException()
			val history = historyRepository.getOne(manga) ?: throw EmptyHistoryException()
			val state = ReaderState(
				manga.source.repository.getDetails(manga),
				history.chapterId, history.page, history.scroll
			)
			onOpenReader.call(state)
		}
	}
}