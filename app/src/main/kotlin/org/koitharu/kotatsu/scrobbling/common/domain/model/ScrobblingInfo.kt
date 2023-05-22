package org.koitharu.kotatsu.scrobbling.common.domain.model

import org.koitharu.kotatsu.list.ui.model.ListModel

class ScrobblingInfo(
	val scrobbler: ScrobblerService,
	val mangaId: Long,
	val targetId: Long,
	val status: ScrobblingStatus?,
	val chapter: Int,
	val comment: String?,
	val rating: Float,
	val title: String,
	val coverUrl: String,
	val description: CharSequence?,
	val externalUrl: String,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ScrobblingInfo

		if (scrobbler != other.scrobbler) return false
		if (mangaId != other.mangaId) return false
		if (targetId != other.targetId) return false
		if (status != other.status) return false
		if (chapter != other.chapter) return false
		if (comment != other.comment) return false
		if (rating != other.rating) return false
		if (title != other.title) return false
		if (coverUrl != other.coverUrl) return false
		if (description != other.description) return false
		if (externalUrl != other.externalUrl) return false

		return true
	}

	override fun hashCode(): Int {
		var result = scrobbler.hashCode()
		result = 31 * result + mangaId.hashCode()
		result = 31 * result + targetId.hashCode()
		result = 31 * result + (status?.hashCode() ?: 0)
		result = 31 * result + chapter
		result = 31 * result + (comment?.hashCode() ?: 0)
		result = 31 * result + rating.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + coverUrl.hashCode()
		result = 31 * result + (description?.hashCode() ?: 0)
		result = 31 * result + externalUrl.hashCode()
		return result
	}
}
