package org.koitharu.kotatsu.favourites.domain

@Deprecated("Use flow")
fun interface OnFavouritesChangeListener {

	fun onFavouritesChanged(mangaId: Long)

	fun onCategoriesChanged() = Unit
}