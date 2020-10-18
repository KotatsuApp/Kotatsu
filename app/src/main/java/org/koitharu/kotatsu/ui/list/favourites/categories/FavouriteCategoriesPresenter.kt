package org.koitharu.kotatsu.ui.list.favourites.categories

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moxy.InjectViewState
import org.koin.core.component.get
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.ui.base.BasePresenter

@InjectViewState
class FavouriteCategoriesPresenter : BasePresenter<FavouriteCategoriesView>() {

	private val repository = get<FavouritesRepository>()
	private val reorderMutex by lazy(LazyThreadSafetyMode.NONE) { Mutex() }

	override fun onFirstViewAttach() {
		super.onFirstViewAttach()
		loadAllCategories()
	}

	fun loadAllCategories() {
		launchJob {
			val categories = repository.getAllCategories()
			viewState.onCategoriesChanged(categories)
		}
	}

	fun loadMangaCategories(manga: Manga) {
		launchJob {
			val categories = repository.getCategories(manga.id)
			viewState.onCheckedCategoriesChanged(categories.map { it.id.toInt() }.toSet())
		}
	}

	fun createCategory(name: String) {
		launchJob {
			repository.addCategory(name)
			val categories = repository.getAllCategories()
			viewState.onCategoriesChanged(categories)
		}
	}

	fun renameCategory(id: Long, name: String) {
		launchJob {
			repository.renameCategory(id, name)

		}
	}

	fun deleteCategory(id: Long) {
		launchJob {
			repository.removeCategory(id)
			val categories = repository.getAllCategories()
			viewState.onCategoriesChanged(categories)
		}
	}

	fun storeCategoriesOrder(orderedIds: List<Long>) {
		launchJob {
			reorderMutex.withLock {
				repository.reorderCategories(orderedIds)
			}
		}
	}

	fun addToCategory(manga: Manga, categoryId: Long) {
		launchJob {
			repository.addToCategory(manga, categoryId)
		}
	}

	fun removeFromCategory(manga: Manga, categoryId: Long) {
		launchJob {
			repository.removeFromCategory(manga, categoryId)
		}
	}
}