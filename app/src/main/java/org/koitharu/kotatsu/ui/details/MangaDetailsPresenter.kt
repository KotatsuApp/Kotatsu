package org.koitharu.kotatsu.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaInfo
import org.koitharu.kotatsu.domain.HistoryRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class MangaDetailsPresenter : BasePresenter<MangaDetailsView>() {

	private var isLoaded = false

	fun loadDetails(manga: Manga) {
		if (isLoaded) {
			return
		}
		viewState.onMangaUpdated(MangaInfo(manga, null))
		launch {
			try {
				viewState.onLoadingStateChanged(true)
				val data = withContext(Dispatchers.IO) {
					val details = async {
						MangaProviderFactory.create(manga.source).getDetails(manga)
					}
					val history = async {
						HistoryRepository().use { it.getHistory(manga) }
					}
					MangaInfo(details.await(), history.await())
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
}