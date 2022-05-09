package org.koitharu.kotatsu.shikimori.data.model

import org.json.JSONObject
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl

class ShikimoriManga(
	val id: Long,
	val name: String,
	val altName: String?,
	val cover: String,
	val url: String,
) : ListModel {

	constructor(json: JSONObject) : this(
		id = json.getLong("id"),
		name = json.getString("name"),
		altName = json.getStringOrNull("russian"),
		cover = json.getJSONObject("image").getString("preview").toAbsoluteUrl("shikimori.one"),
		url = json.getString("url").toAbsoluteUrl("shikimori.one"),
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ShikimoriManga

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
		return "ShikimoriManga #$id \"$name\" $url"
	}
}