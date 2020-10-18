package org.koitharu.kotatsu.domain.favourites

fun interface OnFavouritesChangeListener {

	fun onFavouritesChanged(mangaId: Long)

	fun onCategoriesChanged() = Unit
}