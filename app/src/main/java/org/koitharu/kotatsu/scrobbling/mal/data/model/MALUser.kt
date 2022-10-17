package org.koitharu.kotatsu.scrobbling.mal.data.model

import org.json.JSONObject

class MALUser(
	val id: Long,
	val nickname: String,
) {

	constructor(json: JSONObject) : this(
		id = json.getLong("id"),
		nickname = json.getString("name"),
	)

	fun toJson() = JSONObject().apply {
		put("id", id)
		put("nickname", nickname)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MALUser

		if (id != other.id) return false
		if (nickname != other.nickname) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + nickname.hashCode()
		return result
	}
}
