package org.koitharu.kotatsu.scrobbling.common.domain.model

import org.koitharu.kotatsu.list.ui.model.ListModel

data class ScrobblerManga(
	val id: Long,
	val name: String,
	val altName: String?,
	val cover: String,
	val url: String,
) : ListModel {
	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ScrobblerManga && other.id == id
	}

	override fun toString(): String {
		return "ScrobblerManga #$id \"$name\" $url"
	}
}
