package org.koitharu.kotatsu.ui.main.list.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.main.list.MangaListView
import java.io.File

@InjectViewState
class LocalListPresenter  : BasePresenter<MangaListView<File>>() {

	private lateinit var repository: LocalMangaRepository

	override fun onFirstViewAttach() {
		repository = MangaProviderFactory.create(MangaSource.LOCAL) as LocalMangaRepository
		super.onFirstViewAttach()
	}

	fun loadList() {
		launch {
			viewState.onLoadingChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					repository.getList(0)
				}
				viewState.onListChanged(list)
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