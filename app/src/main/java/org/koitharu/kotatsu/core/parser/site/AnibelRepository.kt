package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.util.*

class AnibelRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.ANIBEL

	override val defaultDomain = "anibel.net"

	override val sortOrders: Set<SortOrder> = EnumSet.of(
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
		val page = (offset / 12f).toIntUp().inc()
		val link = when {
			tag != null -> "/manga?genre[]=${tag.key}&page=$page".withDomain()
			else -> "/manga?page=$page".withDomain()
		}
		val doc = loaderContext.httpGet(link).parseHtml()
		val root = doc.body().select("div.manga-block") ?: throw ParseException("Cannot find root")
		val items = root.select("div.anime-card")
		return items.mapNotNull { card ->
			val href = card.selectFirst("a").attr("href")
			val status = card.select("tr")[2].text()
			val fullTitle = card.selectFirst("h1.anime-card-title").text()
				.substringBeforeLast('[')
			val titleParts = fullTitle.splitTwoParts('/')
			Manga(
				id = generateUid(href),
				title = titleParts?.first?.trim() ?: fullTitle,
				coverUrl = card.selectFirst("img").attr("data-src").withDomain(),
				altTitle = titleParts?.second?.trim(),
				author = null,
				rating = Manga.NO_RATING,
				url = href,
				publicUrl = href.withDomain(),
				tags = card.select("p.tupe.tag")?.select("a")?.mapNotNullToSet tags@{ x ->
					MangaTag(
						title = x.text(),
						key = x.attr("href")?.substringAfterLast("=") ?: return@tags null,
						source = source
					)
				}.orEmpty(),
				state = when (status) {
					"выпускаецца" -> MangaState.ONGOING
					"завершанае" -> MangaState.FINISHED
					else -> null
				},
				source = source
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.httpGet(manga.publicUrl).parseHtml()
		val root = doc.body().select("div.container") ?: throw ParseException("Cannot find root")
		return manga.copy(
			description = root.select("div.manga-block.grid-12")[2].select("p").text(),
			chapters = root.select("ul.series").flatMap { table ->
				table.select("li")
			}.map { it.selectFirst("a") }.mapIndexedNotNull { i, a ->
				val href = a.select("a").first().attr("href").toRelativeUrl(getDomain())
				MangaChapter(
					id = generateUid(href),
					name = a.select("a").first().text(),
					number = i + 1,
					url = href,
					source = source
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		val scripts = doc.select("script")
		for (script in scripts) {
			val data = script.html()
			val pos = data.indexOf("dataSource")
			if (pos == -1) {
				continue
			}
			val json = data.substring(pos).substringAfter('[').substringBefore(']')
			val domain = getDomain()
			return json.split(",").mapNotNull {
				it.trim()
					.removeSurrounding('"', '\'')
					.toRelativeUrl(domain)
					.takeUnless(String::isBlank)
			}.map { url ->
				MangaPage(
					id = generateUid(url),
					url = url,
					referer = fullUrl,
					source = source
				)
			}
		}
		throw ParseException("Pages list not found at ${chapter.url.withDomain()}")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val doc = loaderContext.httpGet("https://${getDomain()}/manga").parseHtml()
		val root = doc.body().select("div#tabs-genres").select("ul#list.ul-three-colums")
		return root.select("p.menu-tags.tupe").mapToSet { a ->
			MangaTag(
				title = a.select("a").text().capitalize(Locale.ROOT),
				key = a.select("a").attr("data-name"),
				source = source
			)
		}
	}

	private suspend fun search(query: String): List<Manga> {
		val domain = getDomain()
		val doc = loaderContext.httpGet("https://$domain/search?q=$query").parseHtml()
		val root = doc.body().select("div.manga-block").select("article.tab-2") ?: throw ParseException("Cannot find root")
		val items = root.select("div.anime-card")
		return items.mapNotNull { card ->
			val href = card.select("a").attr("href")
			val status = card.select("tr")[2].text()
			val fullTitle = card.selectFirst("h1.anime-card-title").text()
				.substringBeforeLast('[')
			val titleParts = fullTitle.splitTwoParts('/')
			Manga(
				id = generateUid(href),
				title = titleParts?.first?.trim() ?: fullTitle,
				coverUrl = card.selectFirst("img").attr("src").withDomain(),
				altTitle = titleParts?.second?.trim(),
				author = null,
				rating = Manga.NO_RATING,
				url = href,
				publicUrl = href.withDomain(),
				tags = card.select("p.tupe.tag")?.select("a")?.mapNotNullToSet tags@{ x ->
					MangaTag(
						title = x.text(),
						key = x.attr("href")?.substringAfterLast("=") ?: return@tags null,
						source = source
					)
				}.orEmpty(),
				state = when (status) {
					"выпускаецца" -> MangaState.ONGOING
					"завершанае" -> MangaState.FINISHED
					else -> null
				},
				source = source
			)
		}
	}

}