package org.koitharu.kotatsu.ui.main.list.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaInfo
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.main.list.MangaListView

@InjectViewState
class RemoteListPresenter : BasePresenter<MangaListView<Unit>>() {

	fun loadList(source: MangaSource, offset: Int) {
		launch {
			viewState.onLoadingChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(source)
						.getList(offset)
						.map { MangaInfo(it, Unit) }
				}
				if (offset == 0) {
					viewState.onListChanged(list)
				} else {
					viewState.onListAppended(list)
				}
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingChanged(false)
			}
		}
	}
}