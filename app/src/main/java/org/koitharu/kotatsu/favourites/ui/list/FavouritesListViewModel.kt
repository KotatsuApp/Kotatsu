package org.koitharu.kotatsu.favourites.ui.list

import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel

class FavouritesListViewModel(
	private val categoryId: Long,
	private val repository: FavouritesRepository
) : MangaListViewModel() {

	fun loadList(offset: Int) {
		launchLoadingJob {
			val list = if (categoryId == 0L) {
				repository.getAllManga(offset = offset)
			} else {
				repository.getManga(categoryId = categoryId, offset = offset)
			}
			if (offset == 0) {
				content.value = list
			} else {
				content.value = content.value.orEmpty() + list
			}
		}
	}

	fun removeFromFavourites(manga: Manga) {
		launchJob {
			if (categoryId == 0L) {
				repository.removeFromFavourites(manga)
			} else {
				repository.removeFromCategory(manga, categoryId)
			}
			content.value = content.value?.filterNot { it.id == manga.id }
		}
	}
}