package org.koitharu.kotatsu.bookmarks.domain

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.local.data.hasImageExtension
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import java.time.Instant

data class Bookmark(
	val manga: Manga,
	val pageId: Long,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Instant,
	val percent: Float,
) : ListModel {

	val directImageUrl: String?
		get() = if (isImageUrlDirect()) imageUrl else null

	val imageLoadData: Any
		get() = if (isImageUrlDirect()) imageUrl else toMangaPage()

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is Bookmark &&
			manga.id == other.manga.id &&
			chapterId == other.chapterId &&
			page == other.page
	}

	fun toMangaPage() = MangaPage(
		id = pageId,
		url = imageUrl,
		preview = null,
		source = manga.source,
	)

	private fun isImageUrlDirect(): Boolean {
		return hasImageExtension(imageUrl)
	}
}
