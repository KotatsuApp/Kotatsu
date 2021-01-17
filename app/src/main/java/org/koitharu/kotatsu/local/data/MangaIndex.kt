package org.koitharu.kotatsu.local.data

import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.utils.ext.getStringOrNull
import org.koitharu.kotatsu.utils.ext.mapToSet

class MangaIndex(source: String?) {

	private val json: JSONObject = source?.let(::JSONObject) ?: JSONObject()

	fun setMangaInfo(manga: Manga, append: Boolean) {
		json.put("id", manga.id)
		json.put("title", manga.title)
		json.put("title_alt", manga.altTitle)
		json.put("url", manga.url)
		json.put("cover", manga.coverUrl)
		json.put("description", manga.description)
		json.put("rating", manga.rating)
		json.put("source", manga.source.name)
		json.put("cover_large", manga.largeCoverUrl)
		json.put("tags", JSONArray().also { a ->
			for (tag in manga.tags) {
				val jo = JSONObject()
				jo.put("key", tag.key)
				jo.put("title", tag.title)
				a.put(jo)
			}
		})
		if (!append || !json.has("chapters")) {
			json.put("chapters", JSONObject())
		}
		json.put("app_id", BuildConfig.APPLICATION_ID)
		json.put("app_version", BuildConfig.VERSION_CODE)
	}

	fun getMangaInfo(): Manga? = if (json.length() == 0) null else runCatching {
		val source = MangaSource.valueOf(json.getString("source"))
		Manga(
			id = json.getLong("id"),
			title = json.getString("title"),
			altTitle = json.getStringOrNull("title_alt"),
			url = json.getString("url"),
			source = source,
			rating = json.getDouble("rating").toFloat(),
			coverUrl = json.getString("cover"),
			description = json.getStringOrNull("description"),
			tags = json.getJSONArray("tags").mapToSet { x ->
				MangaTag(
					title = x.getString("title"),
					key = x.getString("key"),
					source = source
				)
			},
			chapters = getChapters(json.getJSONObject("chapters"), source)
		)
	}.getOrNull()

	fun getCoverEntry(): String? = json.optString("cover_entry")

	fun addChapter(chapter: MangaChapter) {
		val chapters = json.getJSONObject("chapters")
		if (!chapters.has(chapter.id.toString())) {
			val jo = JSONObject()
			jo.put("number", chapter.number)
			jo.put("url", chapter.url)
			jo.put("name", chapter.name)
			jo.put("entries", "%03d\\d{3}".format(chapter.number))
			chapters.put(chapter.id.toString(), jo)
		}
	}

	fun setCoverEntry(name: String) {
		json.put("cover_entry", name)
	}

	fun getChapterNamesPattern(chapter: MangaChapter) = Regex(
		json.getJSONObject("chapters")
			.getJSONObject(chapter.id.toString())
			.getString("entries")
	)

	private fun getChapters(json: JSONObject, source: MangaSource): List<MangaChapter> {
		val chapters = ArrayList<MangaChapter>(json.length())
		for (k in json.keys()) {
			val v = json.getJSONObject(k)
			chapters.add(
				MangaChapter(
					id = k.toLong(),
					name = v.getString("name"),
					url = v.getString("url"),
					number = v.getInt("number"),
					source = source
				)
			)
		}
		return chapters.sortedBy { it.number }
	}

	override fun toString(): String = if (BuildConfig.DEBUG) {
		json.toString(4)
	} else {
		json.toString()
	}
}