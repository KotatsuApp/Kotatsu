package org.koitharu.kotatsu.ui.main.list.favourites.categories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.ui.common.BasePresenter

@InjectViewState
class FavouriteCategoriesPresenter : BasePresenter<FavouriteCategoriesView>() {

	private lateinit var repository: FavouritesRepository

	override fun onFirstViewAttach() {
		repository = FavouritesRepository()
		super.onFirstViewAttach()
		loadAllCategories()
	}

	fun loadAllCategories() {
		presenterScope.launch {
			try {
				val categories = withContext(Dispatchers.IO) {
					repository.getAllCategories()
				}
				viewState.onCategoriesChanged(categories)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			}
		}
	}

	fun loadMangaCategories(manga: Manga) {
		presenterScope.launch {
			try {
				val categories = withContext(Dispatchers.IO) {
					repository.getCategories(manga.id)
				}
				viewState.onCheckedCategoriesChanged(categories.map { it.id.toInt() }.toSet())
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			}
		}
	}

	fun createCategory(name: String) {
		presenterScope.launch {
			try {
				val categories = withContext(Dispatchers.IO) {
					repository.addCategory(name)
					repository.getAllCategories()
				}
				viewState.onCategoriesChanged(categories)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			}
		}
	}

	fun addToCategory(manga: Manga, categoryId: Long) {
		presenterScope.launch {
			try {
				val categories = withContext(Dispatchers.IO) {
					repository.addToCategory(manga,categoryId)
				}
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			}
		}
	}

	fun removeFromCategory(manga: Manga, categoryId: Long) {
		presenterScope.launch {
			try {
				val categories = withContext(Dispatchers.IO) {
					repository.removeFromCategory(manga, categoryId)
				}
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			}
		}
	}
}