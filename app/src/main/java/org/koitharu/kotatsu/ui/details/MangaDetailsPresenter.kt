package org.koitharu.kotatsu.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.domain.history.OnHistoryChangeListener
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class MangaDetailsPresenter : BasePresenter<MangaDetailsView>(), OnHistoryChangeListener {

	private lateinit var historyRepository: HistoryRepository

	private var manga: Manga? = null

	override fun onFirstViewAttach() {
		historyRepository = HistoryRepository()
		super.onFirstViewAttach()
		HistoryRepository.subscribe(this)
	}

	fun loadDetails(manga: Manga, force: Boolean = false) {
		if (!force && this.manga == manga) {
			return
		}
		loadHistory(manga)
		viewState.onMangaUpdated(manga)
		launch {
			try {
				viewState.onLoadingStateChanged(true)
				val data = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(manga.source).getDetails(manga)
				}
				viewState.onMangaUpdated(data)
				this@MangaDetailsPresenter.manga = data
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

	private fun loadHistory(manga: Manga) {
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

	override fun onHistoryChanged() {
		loadHistory(manga ?: return)
	}

	override fun onDestroy() {
		HistoryRepository.unsubscribe(this)
		super.onDestroy()
	}
}