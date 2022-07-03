package org.koitharu.kotatsu.scrobbling.shikimori.data.model

import org.json.JSONObject

class ShikimoriUser(
	val id: Long,
	val nickname: String,
	val avatar: String,
) {

	constructor(json: JSONObject) : this(
		id = json.getLong("id"),
		nickname = json.getString("nickname"),
		avatar = json.getString("avatar"),
	)

	fun toJson() = JSONObject().apply {
		put("id", id)
		put("nickname", nickname)
		put("avatar", avatar)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ShikimoriUser

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