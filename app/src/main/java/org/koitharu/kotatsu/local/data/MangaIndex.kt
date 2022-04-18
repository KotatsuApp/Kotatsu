package org.koitharu.kotatsu.local.data

import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.json.getBooleanOrDefault
import org.koitharu.kotatsu.parsers.util.json.getLongOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase

class MangaIndex(source: String?) {

	private val json: JSONObject = source?.let(::JSONObject) ?: JSONObject()

	fun setMangaInfo(manga: Manga, append: Boolean) {
		json.put("id", manga.id)
		json.put("title", manga.title)
		json.put("title_alt", manga.altTitle)
		json.put("url", manga.url)
		json.put("public_url", manga.publicUrl)
		json.put("author", manga.author)
		json.put("cover", manga.coverUrl)
		json.put("description", manga.description)
		json.put("rating", manga.rating)
		json.put("nsfw", manga.isNsfw)
		json.put("state", manga.state?.name)
		json.put("source", manga.source.name)
		json.put("cover_large", manga.largeCoverUrl)
		json.put(
			"tags",
			JSONArray().also { a ->
				for (tag in manga.tags) {
					val jo = JSONObject()
					jo.put("key", tag.key)
					jo.put("title", tag.title)
					a.put(jo)
				}
			}
		)
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
			publicUrl = json.getStringOrNull("public_url").orEmpty(),
			author = json.getStringOrNull("author"),
			largeCoverUrl = json.getStringOrNull("cover_large"),
			source = source,
			rating = json.getDouble("rating").toFloat(),
			isNsfw = json.getBooleanOrDefault("nsfw", false),
			coverUrl = json.getString("cover"),
			state = json.getStringOrNull("state")?.let { stateString ->
				MangaState.values().find { it.name == stateString }
			},
			description = json.getStringOrNull("description"),
			tags = json.getJSONArray("tags").mapJSONToSet { x ->
				MangaTag(
					title = x.getString("title").toTitleCase(),
					key = x.getString("key"),
					source = source
				)
			},
			chapters = getChapters(json.getJSONObject("chapters"), source),
		)
	}.getOrNull()

	fun getCoverEntry(): String? = json.getStringOrNull("cover_entry")

	fun addChapter(chapter: MangaChapter) {
		val chapters = json.getJSONObject("chapters")
		if (!chapters.has(chapter.id.toString())) {
			val jo = JSONObject()
			jo.put("number", chapter.number)
			jo.put("url", chapter.url)
			jo.put("name", chapter.name)
			jo.put("uploadDate", chapter.uploadDate)
			jo.put("scanlator", chapter.scanlator)
			jo.put("branch", chapter.branch)
			jo.put("entries", "%03d\\d{3}".format(chapter.number))
			chapters.put(chapter.id.toString(), jo)
		}
	}

	fun removeChapter(id: Long): Boolean {
		return json.getJSONObject("chapters").remove(id.toString()) != null
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
					uploadDate = v.getLongOrDefault("uploadDate", 0L),
					scanlator = v.getStringOrNull("scanlator"),
					branch = v.getStringOrNull("branch"),
					source = source,
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