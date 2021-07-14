package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.util.*

class MangareadRepository(
	loaderContext: MangaLoaderContext
) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.MANGAREAD

	override val defaultDomain = "www.mangaread.org"

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY
	)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		if (offset % PAGE_SIZE != 0) {
			return emptyList()
		}
		val payload = createRequestTemplate()
		payload["page"] = (offset / PAGE_SIZE).toString()
		payload["vars[meta_key]"] = when (sortOrder) {
			SortOrder.POPULARITY -> "_wp_manga_views"
			SortOrder.UPDATED -> "_latest_update"
			else -> "_wp_manga_views"
		}
		payload["vars[wp-manga-genre]"] = tag?.key.orEmpty()
		payload["vars[s]"] = query.orEmpty()
		val doc = loaderContext.httpPost(
			"https://${getDomain()}/wp-admin/admin-ajax.php",
			payload
		).parseHtml()
		return doc.select("div.row.c-tabs-item__content").map { div ->
			val href = div.selectFirst("a")?.relUrl("href")
				?: parseFailed("Link not found")
			val summary = div.selectFirst(".tab-summary")
			Manga(
				id = generateUid(href),
				url = href,
				publicUrl = href.inContextOf(div),
				coverUrl = div.selectFirst("img")?.absUrl("src").orEmpty(),
				title = summary?.selectFirst("h3")?.text().orEmpty(),
				rating = div.selectFirst("span.total_votes")?.ownText()
					?.toFloatOrNull()?.div(5f) ?: -1f,
				tags = summary?.selectFirst(".mg_genres")?.select("a")?.mapToSet { a ->
					MangaTag(
						key = a.attr("href").removeSuffix("/").substringAfterLast('/'),
						title = a.text(),
						source = MangaSource.MANGAREAD
					)
				}.orEmpty(),
				author = summary?.selectFirst(".mg_author")?.selectFirst("a")?.ownText(),
				state = when (summary?.selectFirst(".mg_status")?.selectFirst(".summary-content")
					?.ownText()?.trim()) {
					"OnGoing" -> MangaState.ONGOING
					"Completed" -> MangaState.FINISHED
					else -> null
				},
				source = MangaSource.MANGAREAD
			)
		}
	}

	override suspend fun getTags(): Set<MangaTag> {
		val doc = loaderContext.httpGet("https://${getDomain()}/manga/").parseHtml()
		val root = doc.body().selectFirst("header")
			?.selectFirst("ul.second-menu") ?: parseFailed("Root not found")
		return root.select("li").mapNotNullToSet { li ->
			val a = li.selectFirst("a") ?: return@mapNotNullToSet null
			val href = a.attr("href").removeSuffix("/")
				.substringAfterLast("genres/", "")
			if (href.isEmpty()) {
				return@mapNotNullToSet null
			}
			MangaTag(
				key = href,
				title = a.text(),
				source = MangaSource.MANGAREAD
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val fullUrl = manga.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		val root = doc.body().selectFirst("div.profile-manga")
			?.selectFirst("div.summary_content")
			?.selectFirst("div.post-content")
			?: throw ParseException("Root not found")
		val root2 = doc.body().selectFirst("div.content-area")
			?.selectFirst("div.c-page")
			?: throw ParseException("Root2 not found")
		val mangaId = doc.getElementsByAttribute("data-postid").firstOrNull()
			?.attr("data-postid")?.toLongOrNull()
			?: throw ParseException("Cannot obtain manga id")
		val doc2 = loaderContext.httpPost(
			"https://${getDomain()}/wp-admin/admin-ajax.php",
			mapOf(
				"action" to "manga_get_chapters",
				"manga" to mangaId.toString()
			)
		).parseHtml()
		return manga.copy(
			tags = root.selectFirst("div.genres-content")?.select("a")
				?.mapNotNullToSet { a ->
					MangaTag(
						key = a.attr("href").removeSuffix("/").substringAfterLast('/'),
						title = a.text(),
						source = MangaSource.MANGAREAD
					)
				} ?: manga.tags,
			description = root2.selectFirst("div.description-summary")
				?.selectFirst("div.summary__content")
				?.select("p")
				?.filterNot { it.ownText().startsWith("A brief description") }
				?.joinToString { it.html() },
			chapters = doc2.select("li").asReversed().mapIndexed { i, li ->
				val a = li.selectFirst("a")
				val href = a?.relUrl("href").orEmpty().ifEmpty {
					parseFailed("Link is missing")
				}
				MangaChapter(
					id = generateUid(href),
					name = a!!.ownText(),
					number = i + 1,
					url = href,
					source = MangaSource.MANGAREAD
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		val root = doc.body().selectFirst("div.main-col-inner")
			?.selectFirst("div.reading-content")
			?: throw ParseException("Root not found")
		return root.select("div.page-break").map { div ->
			val img = div.selectFirst("img")
			val url = img?.relUrl("src") ?: parseFailed("Page image not found")
			MangaPage(
				id = generateUid(url),
				url = url,
				referer = fullUrl,
				source = MangaSource.MANGAREAD
			)
		}
	}

	private companion object {

		private const val PAGE_SIZE = 12

		private fun createRequestTemplate() =
			"action=madara_load_more&page=1&template=madara-core%2Fcontent%2Fcontent-search&vars%5Bs%5D=&vars%5Borderby%5D=meta_value_num&vars%5Bpaged%5D=1&vars%5Btemplate%5D=search&vars%5Bmeta_query%5D%5B0%5D%5Brelation%5D=AND&vars%5Bmeta_query%5D%5Brelation%5D=OR&vars%5Bpost_type%5D=wp-manga&vars%5Bpost_status%5D=publish&vars%5Bmeta_key%5D=_latest_update&vars%5Border%5D=desc&vars%5Bmanga_archives_item_layout%5D=default"
				.split('&')
				.map {
					val pos = it.indexOf('=')
					it.substring(0, pos) to it.substring(pos + 1)
				}.toMutableMap()
	}
}