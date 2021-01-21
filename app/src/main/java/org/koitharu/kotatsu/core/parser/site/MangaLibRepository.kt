package org.koitharu.kotatsu.core.parser.site

import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.utils.ext.*
import java.util.*
import kotlin.collections.ArrayList

open class MangaLibRepository(loaderContext: MangaLoaderContext) :
	RemoteMangaRepository(loaderContext) {

	protected open val defaultDomain = "mangalib.me"

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
			return search(query)
		}
		val domain = conf.getDomain(defaultDomain)
		val page = (offset / 60f).toIntUp()
		val url = buildString {
			append("https://")
			append(domain)
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
		val items = root.selectFirst("div.media-cards-grid").select("div.media-card-wrap")
		return items.mapNotNull { card ->
			val a = card.selectFirst("a.media-card") ?: return@mapNotNull null
			val href = a.attr("href").withDomain(domain)
			Manga(
				id = href.longHashCode(),
				title = card.selectFirst("h3").text(),
				coverUrl = a.attr("data-src").withDomain(domain),
				altTitle = null,
				author = null,
				rating = Manga.NO_RATING,
				url = href,
				tags = emptySet(),
				state = null,
				source = source
			)
		}
	}

	override fun onCreatePreferences() = arraySetOf(SourceSettings.KEY_DOMAIN)

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.httpGet(manga.url + "?section=info").parseHtml()
		val root = doc.body().getElementById("main-page") ?: throw ParseException("Root not found")
		val title = root.selectFirst("div.media-header__wrap")?.children()
		val info = root.selectFirst("div.media-content")
		val chaptersDoc = loaderContext.httpGet(manga.url + "?section=chapters").parseHtml()
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
						val url = buildString {
							append(manga.url)
							append("/v")
							append(item.getInt("chapter_volume"))
							append("/c")
							append(item.getString("chapter_number"))
							append('/')
							append(item.optString("chapter_string"))
						}
						var name = item.getString("chapter_name")
						if (name.isNullOrBlank() || name == "null") {
							name = "Том " + item.getInt("chapter_volume") +
									" Глава " + item.getString("chapter_number")
						}
						chapters.add(
							MangaChapter(
								id = url.longHashCode(),
								url = url,
								source = source,
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
			author = info.getElementsMatchingOwnText("Автор").firstOrNull()
				?.nextElementSibling()?.text() ?: manga.author,
			tags = info.selectFirst("div.media-tags")
				?.select("a.media-tag-item")?.mapToSet { a ->
					MangaTag(
						title = a.text().capitalize(),
						key = a.attr("href").substringAfterLast('='),
						source = source
					)
				} ?: manga.tags,
			description = info.selectFirst("div.media-description__text")?.html(),
			chapters = chapters
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = loaderContext.httpGet(chapter.url).parseHtml()
		val scripts = doc.head().select("script")
		val pg = doc.body().getElementById("pg").html()
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
						id = pageUrl.longHashCode(),
						url = pageUrl,
						referer = chapter.url,
						source = source
					)
				}
			}
		}
		throw ParseException("Script with info not found")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val domain = conf.getDomain(defaultDomain)
		val url = "https://$domain/manga-list"
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
						title = x.getString("name").capitalize()
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
		val domain = conf.getDomain(defaultDomain)
		val json = loaderContext.httpGet("https://$domain/search?query=${query.urlEncoded()}")
			.parseJsonArray()
		return json.map { jo ->
			val url = "https://$domain/${jo.getString("slug")}"
			Manga(
				id = url.longHashCode(),
				url = url,
				title = jo.getString("rus_name"),
				altTitle = jo.getString("name"),
				author = null,
				tags = emptySet(),
				rating = Manga.NO_RATING,
				state = null,
				source = source,
				coverUrl = "https://$domain/uploads/cover/${jo.getString("slug")}/${jo.getString("cover")}/cover_thumb.jpg"
			)
		}
	}
}