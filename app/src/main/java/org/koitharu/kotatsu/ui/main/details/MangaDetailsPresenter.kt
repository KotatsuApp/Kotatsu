package org.koitharu.kotatsu.ui.main.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class MangaDetailsPresenter : BasePresenter<MangaDetailsView>() {

	private var isLoaded = false

	fun loadDetails(manga: Manga) {
		if (isLoaded) {
			return
		}
		viewState.onMangaUpdated(manga)
		launch {
			try {
				viewState.onLoadingStateChanged(true)
				val details = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(manga.source).getDetails(manga)
				}
				viewState.onMangaUpdated(details)
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