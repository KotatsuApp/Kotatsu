package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.util.*

class MangaOwlRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.MANGAOWL

	override val defaultDomain = "mangaowls.com"

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.UPDATED
	)

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?,
	): List<Manga> {
		val page = (offset / 36f).toIntUp().inc()
		val link = buildString {
			append("https://")
			append(getDomain())
			when {
				!query.isNullOrEmpty() -> {
					append("/search/${page}?search=")
					append(query.urlEncoded())
				}
				!tags.isNullOrEmpty() -> {
					for (tag in tags) {
						append(tag.key)
					}
					append("/${page}?type=${getAlternativeSortKey(sortOrder)}")
				}
				else -> {
					append("/${getSortKey(sortOrder)}/${page}")
				}
			}
		}
		val doc = loaderContext.httpGet(link).parseHtml()
		val slides = doc.body().select("ul.slides") ?: parseFailed("An error occurred while parsing")
		val items = slides.select("div.col-md-2")
		return items.mapNotNull { item ->
			val href = item.select("h6 a").attr("href") ?: return@mapNotNull null
			Manga(
				id = generateUid(href),
				title = item.select("h6 a").text(),
				coverUrl = item.select("div.img-responsive").attr("abs:data-background-image"),
				altTitle = item.select("h6 a").attr("alt") ?: return@mapNotNull null,
				author = null,
				rating = runCatching {
					item.selectFirst("div.block-stars")
						?.text()
						?.toFloatOrNull()
						?.div(10f)
				}.getOrNull() ?: Manga.NO_RATING,
				url = href,
				publicUrl = href.withDomain(),
				source = source
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.httpGet(manga.publicUrl).parseHtml()
		val info = doc.body().selectFirst("div.single_detail") ?: parseFailed("An error occurred while parsing")
		val table = doc.body().selectFirst("div.single-grid-right") ?: parseFailed("An error occurred while parsing")
		return manga.copy(
			description = info.selectFirst(".description")?.html(),
			largeCoverUrl = info.select("img").first()?.let { img ->
				if (img.hasAttr("data-src")) img.attr("abs:data-src") else img.attr("abs:src")
			},
			author = info.select("p.fexi_header_para a.author_link").text(),
			state = parseStatus(info.select("p.fexi_header_para:contains(status)").first()?.ownText()),
			tags = manga.tags + info.select("div.col-xs-12.col-md-8.single-right-grid-right > p > a[href*=genres]")
				.mapNotNull {
					val a = it.selectFirst("a") ?: return@mapNotNull null
					MangaTag(
						title = a.text(),
						key = a.attr("href"),
						source = source
					)
				},
			chapters = table.select("div.table.table-chapter-list").select("li.list-group-item.chapter_list").asReversed().mapIndexed { i, li ->
				val a = li.select("a")
				val href = a.attr("href").ifEmpty {
					parseFailed("Link is missing")
				}
				MangaChapter(
					id = generateUid(href),
					name = a.select("label").text(),
					number = i + 1,
					url = href,
					source = MangaSource.MANGAOWL
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		val root = doc.body().select("div.item img.owl-lazy") ?: throw ParseException("Root not found")
		return root.map { div ->
			val url = div?.attr("abs:data-src") ?: parseFailed("Page image not found")
			MangaPage(
				id = generateUid(url),
				url = url,
				referer = fullUrl,
				source = MangaSource.MANGAOWL
			)
		}
	}

	private fun parseStatus(status: String?) = when {
		status == null -> null
		status.contains("Ongoing") -> MangaState.ONGOING
		status.contains("Completed") -> MangaState.FINISHED
		else -> null
	}

	override suspend fun getTags(): Set<MangaTag> {
		val doc = loaderContext.httpGet("https://${getDomain()}/").parseHtml()
		val root = doc.body().select("ul.dropdown-menu.multi-column.columns-3").select("li")
		return root.mapToSet { p ->
			val a = p.selectFirst("a") ?: parseFailed("a is null")
			MangaTag(
				title = a.text().toCamelCase(),
				key = a.attr("href"),
				source = source
			)
		}
	}

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.POPULARITY -> "popular"
			SortOrder.NEWEST -> "new_release"
			SortOrder.UPDATED -> "lastest"
			else -> "lastest"
		}

	private fun getAlternativeSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.POPULARITY -> "0"
			SortOrder.NEWEST -> "2"
			SortOrder.UPDATED -> "3"
			else -> "3"
		}

}