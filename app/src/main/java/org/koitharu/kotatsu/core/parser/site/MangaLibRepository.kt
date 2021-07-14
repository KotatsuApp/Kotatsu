package org.koitharu.kotatsu.core.parser.site

import androidx.collection.ArraySet
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.AuthRequiredException
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.util.*

open class MangaLibRepository(loaderContext: MangaLoaderContext) :
	RemoteMangaRepository(loaderContext) {

	override val defaultDomain = "mangalib.me"

	override val source = MangaSource.MANGALIB

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.RATING,
		SortOrder.ALPHABETICAL,
		SortOrder.POPULARITY,
		SortOrder.UPDATED,
		SortOrder.NEWEST
	)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		if (!query.isNullOrEmpty()) {
			return if (offset == 0) search(query) else emptyList()
		}
		val page = (offset / 60f).toIntUp()
		val url = buildString {
			append("https://")
			append(getDomain())
			append("/manga-list?dir=")
			append(getSortKey(sortOrder))
			append("&page=")
			append(page)
			if (tag != null) {
				append("&includeGenres[]=")
				append(tag.key)
			}
		}
		val doc = loaderContext.httpGet(url).parseHtml()
		val root = doc.body().getElementById("manga-list") ?: throw ParseException("Root not found")
		val items = root.selectFirst("div.media-cards-grid")?.select("div.media-card-wrap")
			?: return emptyList()
		return items.mapNotNull { card ->
			val a = card.selectFirst("a.media-card") ?: return@mapNotNull null
			val href = a.relUrl("href")
			Manga(
				id = generateUid(href),
				title = card.selectFirst("h3")?.text().orEmpty(),
				coverUrl = a.absUrl("data-src"),
				altTitle = null,
				author = null,
				rating = Manga.NO_RATING,
				url = href,
				publicUrl = href.inContextOf(a),
				tags = emptySet(),
				state = null,
				source = source
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val fullUrl = manga.url.withDomain()
		val doc = loaderContext.httpGet("$fullUrl?section=info").parseHtml()
		val root = doc.body().getElementById("main-page") ?: throw ParseException("Root not found")
		val title = root.selectFirst("div.media-header__wrap")?.children()
		val info = root.selectFirst("div.media-content")
		val chaptersDoc = loaderContext.httpGet("$fullUrl?section=chapters").parseHtml()
		val scripts = chaptersDoc.select("script")
		var chapters: ArrayList<MangaChapter>? = null
		scripts@ for (script in scripts) {
			val raw = script.html().lines()
			for (line in raw) {
				if (line.startsWith("window.__DATA__")) {
					val json = JSONObject(line.substringAfter('=').substringBeforeLast(';'))
					val list = json.getJSONObject("chapters").getJSONArray("list")
					val total = list.length()
					chapters = ArrayList(total)
					for (i in 0 until total) {
						val item = list.getJSONObject(i)
						val chapterId = item.getLong("chapter_id")
						val branchName = item.getStringOrNull("username")
						val url = buildString {
							append(manga.url)
							append("/v")
							append(item.getInt("chapter_volume"))
							append("/c")
							append(item.getString("chapter_number"))
							@Suppress("BlockingMethodInNonBlockingContext") // lint issue
							append('/')
							append(item.optString("chapter_string"))
						}
						var name = item.getStringOrNull("chapter_name")
						if (name.isNullOrBlank() || name == "null") {
							name = "Том " + item.getInt("chapter_volume") +
									" Глава " + item.getString("chapter_number")
						}
						chapters.add(
							MangaChapter(
								id = generateUid(chapterId),
								url = url,
								source = source,
								branch = branchName,
								number = total - i,
								name = name
							)
						)
					}
					chapters.reverse()
					break@scripts
				}
			}
		}
		return manga.copy(
			title = title?.getOrNull(0)?.text()?.takeUnless(String::isBlank) ?: manga.title,
			altTitle = title?.getOrNull(1)?.text()?.substringBefore('/')?.trim(),
			rating = root.selectFirst("div.media-stats-item__score")
				?.selectFirst("span")
				?.text()?.toFloatOrNull()?.div(5f) ?: manga.rating,
			author = info?.getElementsMatchingOwnText("Автор")?.firstOrNull()
				?.nextElementSibling()?.text() ?: manga.author,
			tags = info?.selectFirst("div.media-tags")
				?.select("a.media-tag-item")?.mapToSet { a ->
					MangaTag(
						title = a.text().toCamelCase(),
						key = a.attr("href").substringAfterLast('='),
						source = source
					)
				} ?: manga.tags,
			description = info?.selectFirst("div.media-description__text")?.html(),
			chapters = chapters
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		if (doc.location().endsWith("/register")) {
			throw AuthRequiredException("/login".inContextOf(doc))
		}
		val scripts = doc.head().select("script")
		val pg = (doc.body().getElementById("pg")?.html() ?: parseFailed("Element #pg not found"))
			.substringAfter('=')
			.substringBeforeLast(';')
		val pages = JSONArray(pg)
		for (script in scripts) {
			val raw = script.html().trim()
			if (raw.contains("window.__info")) {
				val json = JSONObject(
					raw.substringAfter("window.__info")
						.substringAfter('=')
						.substringBeforeLast(';')
				)
				val domain = json.getJSONObject("servers").run {
					getStringOrNull("main") ?: getString(
						json.getJSONObject("img").getString("server")
					)
				}
				val url = json.getJSONObject("img").getString("url")
				return pages.map { x ->
					val pageUrl = "$domain/$url${x.getString("u")}"
					MangaPage(
						id = generateUid(pageUrl),
						url = pageUrl,
						referer = fullUrl,
						source = source
					)
				}
			}
		}
		throw ParseException("Script with info not found")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val url = "https://${getDomain()}/manga-list"
		val doc = loaderContext.httpGet(url).parseHtml()
		val scripts = doc.body().select("script")
		for (script in scripts) {
			val raw = script.html().trim()
			if (raw.startsWith("window.__DATA")) {
				val json = JSONObject(raw.substringAfter('=').substringBeforeLast(';'))
				val genres = json.getJSONObject("filters").getJSONArray("genres")
				val result = ArraySet<MangaTag>(genres.length())
				for (x in genres) {
					result += MangaTag(
						source = source,
						key = x.getInt("id").toString(),
						title = x.getString("name").toCamelCase()
					)
				}
				return result
			}
		}
		throw ParseException("Script with genres not found")
	}

	private fun getSortKey(sortOrder: SortOrder?) = when (sortOrder) {
		SortOrder.RATING -> "desc&sort=rate"
		SortOrder.ALPHABETICAL -> "asc&sort=name"
		SortOrder.POPULARITY -> "desc&sort=views"
		SortOrder.UPDATED -> "desc&sort=last_chapter_at"
		SortOrder.NEWEST -> "desc&sort=created_at"
		else -> "desc&sort=last_chapter_at"
	}

	private suspend fun search(query: String): List<Manga> {
		val domain = getDomain()
		val json = loaderContext.httpGet("https://$domain/search?type=manga&q=$query")
			.parseJsonArray()
		return json.map { jo ->
			val slug = jo.getString("slug")
			val url = "/$slug"
			val covers = jo.getJSONObject("covers")
			Manga(
				id = generateUid(url),
				url = url,
				publicUrl = "https://$domain/$slug",
				title = jo.getString("rus_name"),
				altTitle = jo.getString("name"),
				author = null,
				tags = emptySet(),
				rating = jo.getString("rate_avg")
					.toFloatOrNull()?.div(5f) ?: Manga.NO_RATING,
				state = null,
				source = source,
				coverUrl = "https://$domain${covers.getString("thumbnail")}",
				largeCoverUrl = "https://$domain${covers.getString("default")}"
			)
		}
	}
}