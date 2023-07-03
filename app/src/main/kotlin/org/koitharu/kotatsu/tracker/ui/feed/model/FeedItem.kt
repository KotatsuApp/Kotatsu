package org.koitharu.kotatsu.tracker.ui.feed.model

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

class FeedItem(
	val id: Long,
	val imageUrl: String,
	val title: String,
	val manga: Manga,
	val count: Int,
	val isNew: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FeedItem && other.id == id
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as FeedItem

		if (id != other.id) return false
		if (imageUrl != other.imageUrl) return false
		if (title != other.title) return false
		if (manga != other.manga) return false
		if (count != other.count) return false
		return isNew == other.isNew
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + imageUrl.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + manga.hashCode()
		result = 31 * result + count
		result = 31 * result + isNew.hashCode()
		return result
	}
}
