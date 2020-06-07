package org.koitharu.kotatsu.ui.details

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.domain.MangaDataRepository
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.MangaSearchRepository
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.domain.favourites.OnFavouritesChangeListener
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.domain.history.OnHistoryChangeListener
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.utils.ext.safe
import java.io.IOException

@InjectViewState
class MangaDetailsPresenter private constructor() : BasePresenter<MangaDetailsView>(),
	OnHistoryChangeListener,
	OnFavouritesChangeListener {

	private lateinit var historyRepository: HistoryRepository
	private lateinit var favouritesRepository: FavouritesRepository
	private lateinit var trackingRepository: TrackingRepository
	private lateinit var searchRepository: MangaSearchRepository

	private var manga: Manga? = null

	override fun onFirstViewAttach() {
		historyRepository = HistoryRepository()
		favouritesRepository = FavouritesRepository()
		trackingRepository = TrackingRepository()
		searchRepository = MangaSearchRepository()
		super.onFirstViewAttach()
		HistoryRepository.subscribe(this)
		FavouritesRepository.subscribe(this)
	}

	fun findMangaById(id: Long) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val manga = withContext(Dispatchers.IO) {
					MangaDataRepository().findMangaById(id)
				} ?: throw MangaNotFoundException("Cannot find manga by id")
				viewState.onMangaUpdated(manga)
				loadDetails(manga, true)
			} catch (_: CancellationException){
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
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
				viewState.onNewChaptersChanged(trackingRepository.getNewChaptersCount(manga.id))
			} catch (_: CancellationException){
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
	}

	fun deleteLocal(manga: Manga) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				withContext(Dispatchers.IO) {
					val repository =
						MangaProviderFactory.create(MangaSource.LOCAL) as LocalMangaRepository
					val original = repository.getRemoteManga(manga)
					repository.delete(manga) || throw IOException("Unable to delete file")
					safe {
						HistoryRepository().deleteOrSwap(manga, original)
					}
				}
				viewState.onMangaRemoved(manga)
			} catch (e: CancellationException) {
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

	fun loadRelated() {
		val manga = this.manga ?: return
		presenterScope.launch {
			viewState.onLoadingStateChanged(isLoading = true)
			var isFirstCall = true
			searchRepository.globalSearch(manga.title)
				.map { list ->
					list.filter { x -> x.id != manga.id }
				}.filterNot { x -> x.isEmpty() }
				.flowOn(Dispatchers.IO)
				.catch { e ->
					if (e is IOException) {
						viewState.onError(e)
					}
				}
				.onEmpty {
					viewState.onListChanged(emptyList())
					viewState.onLoadingStateChanged(isLoading = false)
				}.onCompletion {
					viewState.onListAppended(emptyList())
				}.collect {
					if (isFirstCall) {
						isFirstCall = false
						viewState.onListChanged(it)
						viewState.onLoadingStateChanged(isLoading = false)
					} else {
						viewState.onListAppended(it)
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

	override fun onCategoriesChanged() = Unit

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