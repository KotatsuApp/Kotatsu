package org.koitharu.kotatsu.bookmarks.domain

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import java.util.Date

class Bookmark(
	val manga: Manga,
	val pageId: Long,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Date,
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
		return imageUrl.substringAfterLast('.').length in 2..4
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Bookmark

		if (manga != other.manga) return false
		if (pageId != other.pageId) return false
		if (chapterId != other.chapterId) return false
		if (page != other.page) return false
		if (scroll != other.scroll) return false
		if (imageUrl != other.imageUrl) return false
		if (createdAt != other.createdAt) return false
		return percent == other.percent
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + pageId.hashCode()
		result = 31 * result + chapterId.hashCode()
		result = 31 * result + page
		result = 31 * result + scroll
		result = 31 * result + imageUrl.hashCode()
		result = 31 * result + createdAt.hashCode()
		result = 31 * result + percent.hashCode()
		return result
	}
}
