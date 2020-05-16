package org.koitharu.kotatsu.domain.favourites

interface OnFavouritesChangeListener {

	fun onFavouritesChanged(mangaId: Long)

	fun onCategoriesChanged()
}