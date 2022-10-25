package org.koitharu.kotatsu.scrobbling.anilist.data.model

import org.json.JSONObject

class AniListUser(
	val id: Long,
	val nickname: String,
	val avatar: String,
) {

	constructor(json: JSONObject) : this(
		id = json.getLong("id"),
		nickname = json.getString("name"),
		avatar = json.getJSONObject("avatar").getString("medium"),
	)

	fun toJson() = JSONObject().apply {
		put("id", id)
		put("name", nickname)
		put("avatar", JSONObject().apply { put("medium", avatar) })
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as AniListUser

		if (id != other.id) return false
		if (nickname != other.nickname) return false
		if (avatar != other.avatar) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + nickname.hashCode()
		result = 31 * result + avatar.hashCode()
		return result
	}
}
