package org.koitharu.kotatsu.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.HistoryRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class MangaDetailsPresenter : BasePresenter<MangaDetailsView>() {

	private lateinit var historyRepository: HistoryRepository

	private var isLoaded = false

	override fun onFirstViewAttach() {
		historyRepository = HistoryRepository()
		super.onFirstViewAttach()
	}

	fun loadDetails(manga: Manga, force: Boolean = false) {
		if (!force && isLoaded) {
			return
		}
		viewState.onMangaUpdated(manga)
		launch {
			try {
				viewState.onLoadingStateChanged(true)
				val data = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(manga.source).getDetails(manga)
				}
				viewState.onMangaUpdated(data)
				isLoaded = true
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
	}

	fun loadHistory(manga: Manga) {
		launch {
			try {
				val history = withContext(Dispatchers.IO) {
					historyRepository.getOne(manga)
				}
				viewState.onHistoryChanged(history)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
			}
		}
	}
}