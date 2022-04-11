package org.koitharu.kotatsu.shikimori.data.model

import org.json.JSONObject

class ShikimoriMangaInfo(
	val id: Long,
	val name: String,
	val cover: String,
	val url: String,
	val descriptionHtml: String,
) {

	constructor(json: JSONObject) : this(
		id = json.getLong("id"),
		name = json.getString("name"),
		cover = json.getJSONObject("image").getString("preview"),
		url = json.getString("url"),
		descriptionHtml = json.getString("description_html"),
	)
}