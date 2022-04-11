package org.koitharu.kotatsu.shikimori.data.model

import org.json.JSONObject

class ShikimoriManga(
	val id: Long,
	val name: String,
	val cover: String,
	val url: String,
) {

	constructor(json: JSONObject) : this(
		id = json.getLong("id"),
		name = json.getString("name"),
		cover = json.getJSONObject("image").getString("preview"),
		url = json.getString("url"),
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ShikimoriManga

		if (id != other.id) return false
		if (name != other.name) return false
		if (cover != other.cover) return false
		if (url != other.url) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + name.hashCode()
		result = 31 * result + cover.hashCode()
		result = 31 * result + url.hashCode()
		return result
	}

	override fun toString(): String {
		return "ShikimoriManga #$id \"$name\" $url"
	}
}