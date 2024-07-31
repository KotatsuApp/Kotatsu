package org.koitharu.kotatsu.local.data

import androidx.annotation.WorkerThread
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.find
import org.koitharu.kotatsu.parsers.util.json.getBooleanOrDefault
import org.koitharu.kotatsu.parsers.util.json.getFloatOrDefault
import org.koitharu.kotatsu.parsers.util.json.getIntOrDefault
import org.koitharu.kotatsu.parsers.util.json.getLongOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import org.koitharu.kotatsu.parsers.util.toTitleCase
import java.io.File

class MangaIndex(source: String?) {

	private val json: JSONObject = source?.let(::JSONObject) ?: JSONObject()

	fun setMangaInfo(manga: Manga) {
		require(!manga.isLocal) { "Local manga information cannot be stored" }
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
			},
		)
		if (!json.has("chapters")) {
			json.put("chapters", JSONObject())
		}
		json.put("app_id", BuildConfig.APPLICATION_ID)
		json.put("app_version", BuildConfig.VERSION_CODE)
	}

	fun getMangaInfo(): Manga? = if (json.length() == 0) null else runCatching {
		val source = MangaSource(json.getString("source"))
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
				MangaState.entries.find(stateString)
			},
			description = json.getStringOrNull("description"),
			tags = json.getJSONArray("tags").mapJSONToSet { x ->
				MangaTag(
					title = x.getString("title").toTitleCase(),
					key = x.getString("key"),
					source = source,
				)
			},
			chapters = getChapters(json.getJSONObject("chapters"), source),
		)
	}.getOrNull()

	fun getCoverEntry(): String? = json.getStringOrNull("cover_entry")

	fun addChapter(chapter: IndexedValue<MangaChapter>, filename: String?) {
		val chapters = json.getJSONObject("chapters")
		if (!chapters.has(chapter.value.id.toString())) {
			val jo = JSONObject()
			jo.put("number", chapter.value.number)
			jo.put("volume", chapter.value.volume)
			jo.put("url", chapter.value.url)
			jo.put("name", chapter.value.name)
			jo.put("uploadDate", chapter.value.uploadDate)
			jo.put("scanlator", chapter.value.scanlator)
			jo.put("branch", chapter.value.branch)
			jo.put("entries", "%08d_%03d\\d{3}".format(chapter.value.branch.hashCode(), chapter.index + 1))
			jo.put("file", filename)
			chapters.put(chapter.value.id.toString(), jo)
		}
	}

	fun removeChapter(id: Long): Boolean {
		return json.has("chapters") && json.getJSONObject("chapters").remove(id.toString()) != null
	}

	fun getChapterFileName(chapterId: Long): String? {
		return json.optJSONObject("chapters")?.optJSONObject(chapterId.toString())?.getStringOrNull("file")
	}

	fun setCoverEntry(name: String) {
		json.put("cover_entry", name)
	}

	fun getChapterNamesPattern(chapter: MangaChapter) = Regex(
		json.getJSONObject("chapters")
			.getJSONObject(chapter.id.toString())
			.getString("entries"),
	)

	fun sortChaptersByName() {
		val jo = json.getJSONObject("chapters")
		val list = ArrayList<JSONObject>(jo.length())
		jo.keys().forEach { id ->
			val item = jo.getJSONObject(id)
			item.put("id", id)
			list.add(item)
		}
		val comparator = org.koitharu.kotatsu.core.util.AlphanumComparator()
		list.sortWith(compareBy(comparator) { it.getString("name") })
		val newJo = JSONObject()
		list.forEachIndexed { i, obj ->
			obj.put("number", i + 1)
			val id = obj.remove("id") as String
			newJo.put(id, obj)
		}
		json.put("chapters", newJo)
	}

	fun clear() {
		val keys = json.keys()
		while (keys.hasNext()) {
			json.remove(keys.next())
		}
	}

	fun setFrom(other: MangaIndex) {
		clear()
		other.json.keys().forEach { key ->
			json.putOpt(key, other.json.opt(key))
		}
	}

	private fun getChapters(json: JSONObject, source: MangaSource): List<MangaChapter> {
		val chapters = ArrayList<MangaChapter>(json.length())
		for (k in json.keys()) {
			val v = json.getJSONObject(k)
			chapters.add(
				MangaChapter(
					id = k.toLong(),
					name = v.getString("name"),
					url = v.getString("url"),
					number = v.getFloatOrDefault("number", 0f),
					volume = v.getIntOrDefault("volume", 0),
					uploadDate = v.getLongOrDefault("uploadDate", 0L),
					scanlator = v.getStringOrNull("scanlator"),
					branch = v.getStringOrNull("branch"),
					source = source,
				),
			)
		}
		return chapters.sortedBy { it.number }
	}

	override fun toString(): String = if (BuildConfig.DEBUG) {
		json.toString(4)
	} else {
		json.toString()
	}

	companion object {

		@WorkerThread
		fun read(file: File): MangaIndex? {
			if (file.exists() && file.canRead()) {
				val text = file.readText()
				if (text.length > 2) {
					return MangaIndex(text)
				}
			}
			return null
		}
	}
}
