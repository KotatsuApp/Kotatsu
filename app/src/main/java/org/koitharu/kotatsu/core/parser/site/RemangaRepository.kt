package org.koitharu.kotatsu.core.parser.site

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.text.SimpleDateFormat
import java.util.*

class RemangaRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.REMANGA

	override val defaultDomain = "remanga.org"

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.RATING,
		SortOrder.NEWEST
	)

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		val domain = getDomain()
		val urlBuilder = StringBuilder()
			.append("https://api.")
			.append(domain)
		if (query != null) {
			urlBuilder.append("/api/search/?query=")
				.append(query.urlEncoded())
		} else {
			urlBuilder.append("/api/search/catalog/?ordering=")
				.append(getSortKey(sortOrder))
			tags?.forEach { tag ->
				urlBuilder.append("&genres=")
				urlBuilder.append(tag.key)
			}
		}
		urlBuilder
			.append("&page=")
			.append((offset / PAGE_SIZE) + 1)
			.append("&count=")
			.append(PAGE_SIZE)
		val content = loaderContext.httpGet(urlBuilder.toString()).parseJson()
			.getJSONArray("content")
		return content.map { jo ->
			val url = "/manga/${jo.getString("dir")}"
			val img = jo.getJSONObject("img")
			Manga(
				id = generateUid(url),
				url = url,
				publicUrl = "https://$domain$url",
				title = jo.getString("rus_name"),
				altTitle = jo.getString("en_name"),
				rating = jo.getString("avg_rating").toFloatOrNull()?.div(10f) ?: Manga.NO_RATING,
				coverUrl = "https://api.$domain${img.getString("mid")}",
				largeCoverUrl = "https://api.$domain${img.getString("high")}",
				author = null,
				tags = jo.optJSONArray("genres")?.mapToSet { g ->
					MangaTag(
						title = g.getString("name"),
						key = g.getInt("id").toString(),
						source = MangaSource.REMANGA
					)
				}.orEmpty(),
				source = MangaSource.REMANGA
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val domain = getDomain()
		val slug = manga.url.find(LAST_URL_PATH_REGEX)
			?: throw ParseException("Cannot obtain slug from ${manga.url}")
		val data = loaderContext.httpGet(
			url = "https://api.$domain/api/titles/$slug/"
		).parseJson()
		val content = try {
			data.getJSONObject("content")
		} catch (e: JSONException) {
			throw ParseException(data.optString("msg"), e)
		}
		val branchId = content.getJSONArray("branches").optJSONObject(0)
			?.getLong("id") ?: throw ParseException("No branches found")
		val chapters = loaderContext.httpGet(
			url = "https://api.$domain/api/titles/chapters/?branch_id=$branchId"
		).parseJson().getJSONArray("content")
		val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
		return manga.copy(
			description = content.getString("description"),
			state = when (content.optJSONObject("status")?.getInt("id")) {
				STATUS_ONGOING -> MangaState.ONGOING
				STATUS_FINISHED -> MangaState.FINISHED
				else -> null
			},
			tags = content.getJSONArray("genres").mapToSet { g ->
				MangaTag(
					title = g.getString("name"),
					key = g.getInt("id").toString(),
					source = MangaSource.REMANGA
				)
			},
			chapters = chapters.mapIndexed { i, jo ->
				val id = jo.getLong("id")
				val name = jo.getString("name").toTitleCase(Locale.ROOT)
				val publishers = jo.getJSONArray("publishers")
				MangaChapter(
					id = generateUid(id),
					url = "/api/titles/chapters/$id/",
					number = chapters.length() - i,
					name = buildString {
						append("Том ")
						append(jo.getString("tome"))
						append(". ")
						append("Глава ")
						append(jo.getString("chapter"))
						if (name.isNotEmpty()) {
							append(" - ")
							append(name)
						}
					},
					uploadDate = dateFormat.tryParse(jo.getString("upload_date")),
					scanlator = publishers.optJSONObject(0)?.getStringOrNull("name"),
					source = MangaSource.REMANGA,
					branch = null,
				)
			}.asReversed()
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val referer = "https://${getDomain()}/"
		val content = loaderContext.httpGet(chapter.url.withDomain(subdomain = "api")).parseJson()
			.getJSONObject("content").getJSONArray("pages")
		val pages = ArrayList<MangaPage>(content.length())
		for (i in 0 until content.length()) {
			when (val item = content.get(i)) {
				is JSONObject -> pages += parsePage(item, referer)
				is JSONArray -> item.mapTo(pages) { parsePage(it, referer) }
				else -> throw ParseException("Unknown json item $item")
			}
		}
		return pages
	}

	override suspend fun getTags(): Set<MangaTag> {
		val domain = getDomain()
		val content = loaderContext.httpGet("https://api.$domain/api/forms/titles/?get=genres")
			.parseJson().getJSONObject("content").getJSONArray("genres")
		return content.mapToSet { jo ->
			MangaTag(
				title = jo.getString("name"),
				key = jo.getInt("id").toString(),
				source = source
			)
		}
	}

	private fun getSortKey(order: SortOrder?) = when (order) {
		SortOrder.UPDATED -> "-chapter_date"
		SortOrder.POPULARITY -> "-rating"
		SortOrder.RATING -> "-votes"
		SortOrder.NEWEST -> "-id"
		else -> "-chapter_date"
	}

	private fun parsePage(jo: JSONObject, referer: String) = MangaPage(
		id = generateUid(jo.getLong("id")),
		url = jo.getString("link"),
		preview = null,
		referer = referer,
		source = source,
	)

	private companion object {

		const val PAGE_SIZE = 30

		const val STATUS_ONGOING = 1
		const val STATUS_FINISHED = 0

		val LAST_URL_PATH_REGEX = Regex("/[^/]+/?$")
	}
}