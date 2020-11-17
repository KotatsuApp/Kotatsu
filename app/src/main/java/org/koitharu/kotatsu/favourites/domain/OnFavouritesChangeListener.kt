package org.koitharu.kotatsu.favourites.domain

fun interface OnFavouritesChangeListener {

	fun onFavouritesChanged(mangaId: Long)

	fun onCategoriesChanged() = Unit
}