package org.koitharu.kotatsu.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.domain.favourites.OnFavouritesChangeListener
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.domain.history.OnHistoryChangeListener
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class MangaDetailsPresenter private constructor(): BasePresenter<MangaDetailsView>(), OnHistoryChangeListener,
	OnFavouritesChangeListener {

	private lateinit var historyRepository: HistoryRepository
	private lateinit var favouritesRepository: FavouritesRepository

	private var manga: Manga? = null

	override fun onFirstViewAttach() {
		historyRepository = HistoryRepository()
		favouritesRepository = FavouritesRepository()
		super.onFirstViewAttach()
		HistoryRepository.subscribe(this)
		FavouritesRepository.subscribe(this)
	}

	fun loadDetails(manga: Manga, force: Boolean = false) {
		if (!force && this.manga == manga) {
			return
		}
		loadHistory(manga)
		viewState.onMangaUpdated(manga)
		loadFavourite(manga)
		presenterScope.launch {
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
		presenterScope.launch {
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

	private fun loadFavourite(manga: Manga) {
		presenterScope.launch {
			try {
				val categories = withContext(Dispatchers.IO) {
					favouritesRepository.getCategories(manga.id)
				}
				viewState.onFavouriteChanged(categories)
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

	override fun onFavouritesChanged(mangaId: Long) {
		if (mangaId == manga?.id) {
			loadFavourite(manga!!)
		}
	}

	override fun onDestroy() {
		HistoryRepository.unsubscribe(this)
		FavouritesRepository.unsubscribe(this)
		instance = null
		super.onDestroy()
	}

	companion object {

		private var instance: MangaDetailsPresenter? = null

		fun getInstance(): MangaDetailsPresenter = instance ?: synchronized(this) {
			MangaDetailsPresenter().also {
				instance = it
			}
		}
	}
}