package org.koitharu.kotatsu.bookmarks.domain

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.local.data.hasImageExtension
import org.koitharu.kotatsu.local.data.isImageExtension
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import java.util.Date

data class Bookmark(
	val manga: Manga,
	val pageId: Long,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Date,
	val percent: Float,
) : ListModel {

	val imageLoadData: Any
		get() = if (hasImageExtension(imageUrl)) imageUrl else toMangaPage()

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
}
