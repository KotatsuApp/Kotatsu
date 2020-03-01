package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.BaseMangaRepository
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.utils.ext.*

abstract class ChanRepository(
	private val source: MangaSource,
	loaderContext: MangaLoaderContext
) : BaseMangaRepository(loaderContext) {

	protected abstract val domain: String

	override val sortOrders = setOf(SortOrder.NEWEST, SortOrder.POPULARITY, SortOrder.ALPHABETICAL)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val url = when {
			query != null -> "https://$domain/?do=search&subaction=search&story=${query.urlEncoded()}"
			tag != null -> "https://$domain/tags/${tag.key}&n=${getSortKey2(sortOrder)}?offset=$offset"
			else -> "https://$domain/${getSortKey(sortOrder)}?offset=$offset"
		}
		val doc = loaderContext.get(url).parseHtml()
		val root = doc.body().selectFirst("div.main_fon").getElementById("content")
			?: throw ParseException("Cannot find root")
		return root.select("div.content_row").mapNotNull { row ->
			val a = row.selectFirst("div.manga_row1")?.selectFirst("a.title_link")
				?: return@mapNotNull null
			val href = a.attr("href").withDomain(domain)
			Manga(
				id = href.longHashCode(),
				url = href,
				altTitle = a.attr("title"),
				title = a.text().substringAfterLast('(').substringBeforeLast(')'),
				author = row.getElementsByAttributeValueStarting(
					"href",
					"/mangaka"
				).firstOrNull()?.text(),
				coverUrl = row.selectFirst("div.manga_images")?.selectFirst("img")
					?.attr("src")?.withDomain(domain).orEmpty(),
				tags = safe {
					row.selectFirst("div.genre")?.select("a")?.map {
						MangaTag(
							title = it.text(),
							key = it.attr("href").substringAfterLast('/').urlEncoded(),
							source = source
						)
					}?.toSet()
				}.orEmpty(),
				source = source
			)
		}
	}


	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.get(manga.url).parseHtml()
		val root =
			doc.body().getElementById("dle-content") ?: throw ParseException("Cannot find root")
		return manga.copy(
			description = root.getElementById("description")?.html()?.substringBeforeLast("<div"),
			largeCoverUrl = root.getElementById("cover")?.attr("src")?.withDomain(domain),
			chapters = root.select("table.table_cha").flatMap { table ->
				table.select("div.manga2")
			}.mapNotNull { it.selectFirst("a") }.reversed().mapIndexedNotNull { i, a ->
				val href = a.attr("href")
					?.withDomain(domain) ?: return@mapIndexedNotNull null
				MangaChapter(
					id = href.longHashCode(),
					name = a.text().trim(),
					number = i + 1,
					url = href,
					source = source
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = loaderContext.get(chapter.url).parseHtml()
		val scripts = doc.select("script")
		for (script in scripts) {
			val data = script.html()
			val pos = data.indexOf("\"fullimg")
			if (pos == -1) {
				continue
			}
			val json = data.substring(pos).substringAfter('[').substringBefore(';')
				.substringBeforeLast(']')
			return json.split(",").mapNotNull {
				it.trim().removeSurrounding('"').takeUnless(String::isBlank)
			}.map { url ->
				MangaPage(
					id = url.longHashCode(),
					url = url,
					source = source
				)
			}
		}
		throw ParseException("Pages list not found at ${chapter.url}")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val doc = loaderContext.get("https://$domain/catalog").parseHtml()
		val root = doc.body().selectFirst("div.main_fon").getElementById("side")
			.select("ul").last()
		return root.select("li.sidetag").map { li ->
			val a = li.children().last()
			MangaTag(
				title = a.text().capitalize(),
				key = a.attr("href").substringAfterLast('/'),
				source = source
			)
		}.toSet()
	}

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minBy { it.ordinal }) {
			SortOrder.ALPHABETICAL -> "catalog"
			SortOrder.POPULARITY -> "mostfavorites"
			SortOrder.NEWEST -> "manga/new"
			else -> "mostfavorites"
		}

	private fun getSortKey2(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minBy { it.ordinal }) {
			SortOrder.ALPHABETICAL -> "abcasc"
			SortOrder.POPULARITY -> "favdesc"
			SortOrder.NEWEST -> "datedesc"
			else -> "favdesc"
		}
}