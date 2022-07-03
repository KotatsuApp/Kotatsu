package org.koitharu.kotatsu.bookmarks.domain

import org.koitharu.kotatsu.parsers.model.Manga
import java.util.*

class Bookmark(
	val manga: Manga,
	val pageId: Long,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Date,
	val percent: Float,
) {

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
		if (percent != other.percent) return false

		return true
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