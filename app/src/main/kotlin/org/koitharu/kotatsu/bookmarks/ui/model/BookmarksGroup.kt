package org.koitharu.kotatsu.bookmarks.ui.model

import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.areItemsEquals

class BookmarksGroup(
	val manga: Manga,
	val bookmarks: List<Bookmark>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is BookmarksGroup && other.manga.id == manga.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is BookmarksGroup && previousState.bookmarks != bookmarks) {
			ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as BookmarksGroup

		if (manga != other.manga) return false

		return bookmarks.areItemsEquals(other.bookmarks) { a, b ->
			a.imageUrl == b.imageUrl
		}
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + bookmarks.sumOf { it.imageUrl.hashCode() }
		return result
	}
}
