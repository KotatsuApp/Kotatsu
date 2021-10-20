package org.koitharu.kotatsu.core.parser.site

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.text.SimpleDateFormat
import java.util.*

abstract class GroupleRepository(loaderContext: MangaLoaderContext) :
	RemoteMangaRepository(loaderContext) {

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.RATING
	)

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		val domain = getDomain()
		val doc = when {
			!query.isNullOrEmpty() -> loaderContext.httpPost(
				"https://$domain/search",
				mapOf(
					"q" to query.urlEncoded(),
					"offset" to (offset upBy PAGE_SIZE_SEARCH).toString()
				)
			)
			tags.isNullOrEmpty() -> loaderContext.httpGet(
				"https://$domain/list?sortType=${
					getSortKey(
						sortOrder
					)
				}&offset=${offset upBy PAGE_SIZE}", HEADER
			)
			tags.size == 1 -> loaderContext.httpGet(
				"https://$domain/list/genre/${tags.first().key}?sortType=${
					getSortKey(
						sortOrder
					)
				}&offset=${offset upBy PAGE_SIZE}", HEADER
			)
			offset > 0 -> return emptyList()
			else -> advancedSearch(domain, tags)
		}.parseHtml().body()
		val root = (doc.getElementById("mangaBox") ?: doc.getElementById("mangaResults"))
			?.selectFirst("div.tiles.row") ?: throw ParseException("Cannot find root")
		val baseHost = root.baseUri().toHttpUrl().host
		return root.select("div.tile").mapNotNull { node ->
			val imgDiv = node.selectFirst("div.img") ?: return@mapNotNull null
			val descDiv = node.selectFirst("div.desc") ?: return@mapNotNull null
			if (descDiv.selectFirst("i.fa-user") != null) {
				return@mapNotNull null //skip author
			}
			val href = imgDiv.selectFirst("a")?.attr("href")?.inContextOf(node)
			if (href == null || href.toHttpUrl().host != baseHost) {
				return@mapNotNull null // skip external links
			}
			val title = descDiv.selectFirst("h3")?.selectFirst("a")?.text()
				?: return@mapNotNull null
			val tileInfo = descDiv.selectFirst("div.tile-info")
			val relUrl = href.toRelativeUrl(baseHost)
			Manga(
				id = generateUid(relUrl),
				url = relUrl,
				publicUrl = href,
				title = title,
				altTitle = descDiv.selectFirst("h4")?.text(),
				coverUrl = imgDiv.selectFirst("img.lazy")?.attr("data-original").orEmpty(),
				rating = runCatching {
					node.selectFirst("div.rating")
						?.attr("title")
						?.substringBefore(' ')
						?.toFloatOrNull()
						?.div(10f)
				}.getOrNull() ?: Manga.NO_RATING,
				author = tileInfo?.selectFirst("a.person-link")?.text(),
				tags = runCatching {
					tileInfo?.select("a.element-link")
						?.mapToSet {
							MangaTag(
								title = it.text(),
								key = it.attr("href").substringAfterLast('/'),
								source = source
							)
						}
				}.getOrNull().orEmpty(),
				state = when {
					node.selectFirst("div.tags")
						?.selectFirst("span.mangaCompleted") != null -> MangaState.FINISHED
					else -> null
				},
				source = source
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.httpGet(manga.url.withDomain(), HEADER).parseHtml()
		val root = doc.body().getElementById("mangaBox")?.selectFirst("div.leftContent")
			?: throw ParseException("Cannot find root")
		val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.US)
		return manga.copy(
			description = root.selectFirst("div.manga-description")?.html(),
			largeCoverUrl = root.selectFirst("div.subject-cower")?.selectFirst("img")?.attr(
				"data-full"
			),
			tags = manga.tags + root.select("div.subject-meta").select("span.elem_genre ")
				.mapNotNull {
					val a = it.selectFirst("a.element-link") ?: return@mapNotNull null
					MangaTag(
						title = a.text(),
						key = a.attr("href").substringAfterLast('/'),
						source = source
					)
				},
			chapters = root.selectFirst("div.chapters-link")?.selectFirst("table")
				?.select("tr:has(td > a)")?.asReversed()?.mapIndexedNotNull { i, tr ->
					val a = tr.selectFirst("a") ?: return@mapIndexedNotNull null
					val href = a.relUrl("href")
					var translators = ""
					val translatorElement = a.attr("title")
					if (!translatorElement.isNullOrBlank()) {
						translators = translatorElement
							.replace("(Переводчик),", "&")
							.removeSuffix(" (Переводчик)")
					}
					MangaChapter(
						id = generateUid(href),
						name = tr.selectFirst("a")?.text().orEmpty().removePrefix(manga.title).trim(),
						number = i + 1,
						url = href,
						uploadDate = dateFormat.tryParse(tr.selectFirst("td.d-none")?.text()),
						scanlator = translators,
						source = source
					)
				}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = loaderContext.httpGet(chapter.url.withDomain() + "?mtr=1", HEADER).parseHtml()
		val scripts = doc.select("script")
		for (script in scripts) {
			val data = script.html()
			val pos = data.indexOf("rm_h.init")
			if (pos == -1) {
				continue
			}
			val json = data.substring(pos).substringAfter('[').substringBeforeLast(']')
			val matches = Regex("\\[.*?]").findAll(json).toList()
			val regex = Regex("['\"].*?['\"]")
			return matches.map { x ->
				val parts = regex.findAll(x.value).toList()
				val url = parts[0].value.removeSurrounding('"', '\'') +
						parts[2].value.removeSurrounding('"', '\'')
				MangaPage(
					id = generateUid(url),
					url = url,
					referer = chapter.url,
					source = source
				)
			}
		}
		throw ParseException("Pages list not found at ${chapter.url}")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val doc = loaderContext.httpGet("https://${getDomain()}/list/genres/sort_name", HEADER).parseHtml()
		val root = doc.body().getElementById("mangaBox")?.selectFirst("div.leftContent")
			?.selectFirst("table.table") ?: parseFailed("Cannot find root")
		return root.select("a.element-link").mapToSet { a ->
			MangaTag(
				title = a.text().toCamelCase(),
				key = a.attr("href").substringAfterLast('/'),
				source = source
			)
		}
	}

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.ALPHABETICAL -> "name"
			SortOrder.POPULARITY -> "rate"
			SortOrder.UPDATED -> "updated"
			SortOrder.NEWEST -> "created"
			SortOrder.RATING -> "votes"
			null -> "updated"
		}

	private suspend fun advancedSearch(domain: String, tags: Set<MangaTag>): Response {
		val url = "https://$domain/search/advanced"
		// Step 1: map catalog genres names to advanced-search genres ids
		val tagsIndex = loaderContext.httpGet(url, HEADER).parseHtml()
			.body().selectFirst("form.search-form")
			?.select("div.form-group")
			?.get(1) ?: parseFailed("Genres filter element not found")
		val tagNames = tags.map { it.title.lowercase() }
		val payload = HashMap<String, String>()
		var foundGenres = 0
		tagsIndex.select("li.property").forEach { li ->
			val name = li.text().trim().lowercase()
			val id = li.selectFirst("input")?.id()
				?: parseFailed("Id for tag $name not found")
			payload[id] = if (name in tagNames) {
				foundGenres++
				"in"
			} else ""
		}
		if (foundGenres != tags.size) {
			parseFailed("Some genres are not found")
		}
		// Step 2: advanced search
		payload["q"] = ""
		payload["s_high_rate"] = ""
		payload["s_single"] = ""
		payload["s_mature"] = ""
		payload["s_completed"] = ""
		payload["s_translated"] = ""
		payload["s_many_chapters"] = ""
		payload["s_wait_upload"] = ""
		payload["s_sale"] = ""
		payload["years"] = "1900,2099"
		payload["+"] = "Искать".urlEncoded()
		return loaderContext.httpPost(url, payload)
	}

	private companion object {

		private const val PAGE_SIZE = 70
		private const val PAGE_SIZE_SEARCH = 50
		private val HEADER = Headers.Builder()
			.add("User-Agent", "readmangafun")
			.build()
	}

}