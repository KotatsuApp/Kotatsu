package org.koitharu.kotatsu.scrobbling.domain.model

import org.koitharu.kotatsu.list.ui.model.ListModel

class ScrobblerManga(
	val id: Long,
	val name: String,
	val altName: String?,
	val cover: String,
	val url: String,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ScrobblerManga

		if (id != other.id) return false
		if (name != other.name) return false
		if (altName != other.altName) return false
		if (cover != other.cover) return false
		if (url != other.url) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + name.hashCode()
		result = 31 * result + altName.hashCode()
		result = 31 * result + cover.hashCode()
		result = 31 * result + url.hashCode()
		return result
	}

	override fun toString(): String {
		return "ScrobblerManga #$id \"$name\" $url"
	}
}