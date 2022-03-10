package org.koitharu.kotatsu.shikimori.data.model

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
}