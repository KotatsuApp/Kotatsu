package org.koitharu.kotatsu.core.parser.site

import androidx.collection.arraySetOf
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.utils.ext.*

abstract class ChanRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(
	loaderContext
) {

	protected abstract val defaultDomain: String

	override val sortOrders = arraySetOf(
		SortOrder.NEWEST,
		SortOrder.POPULARITY,
		SortOrder.ALPHABETICAL
	)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val domain = conf.getDomain(defaultDomain)
		val url = when {
			!query.isNullOrEmpty() -> {
				if (offset != 0) {
					return emptyList()
				}
				"https://$domain/?do=search&subaction=search&story=${query.urlEncoded()}"
			}
			tag != null -> "https://$domain/tags/${tag.key}&n=${getSortKey2(sortOrder)}?offset=$offset"
			else -> "https://$domain/${getSortKey(sortOrder)}?offset=$offset"
		}
		val doc = loaderContext.httpGet(url).parseHtml()
		val root = doc.body().selectFirst("div.main_fon").getElementById("content")
			?: throw ParseException("Cannot find root")
		return root.select("div.content_row").mapNotNull { row ->
			val a = row.selectFirst("div.manga_row1")?.selectFirst("h2")?.selectFirst("a")
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
					row.selectFirst("div.genre")?.select("a")?.mapToSet {
						MangaTag(
							title = it.text(),
							key = it.attr("href").substringAfterLast('/').urlEncoded(),
							source = source
						)
					}
				}.orEmpty(),
				source = source
			)
		}
	}


	override suspend fun getDetails(manga: Manga): Manga {
		val domain = conf.getDomain(defaultDomain)
		val doc = loaderContext.httpGet(manga.url).parseHtml()
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
		val doc = loaderContext.httpGet(chapter.url).parseHtml()
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
				it.trim().removeSurrounding('"', '\'').takeUnless(String::isBlank)
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
		val domain = conf.getDomain(defaultDomain)
		val doc = loaderContext.httpGet("https://$domain/catalog").parseHtml()
		val root = doc.body().selectFirst("div.main_fon").getElementById("side")
			.select("ul").last()
		return root.select("li.sidetag").mapToSet { li ->
			val a = li.children().last()
			MangaTag(
				title = a.text().capitalize(),
				key = a.attr("href").substringAfterLast('/'),
				source = source
			)
		}
	}

	override fun onCreatePreferences() = arraySetOf(SourceSettings.KEY_DOMAIN)

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.ALPHABETICAL -> "catalog"
			SortOrder.POPULARITY -> "mostfavorites"
			SortOrder.NEWEST -> "manga/new"
			else -> "mostfavorites"
		}

	private fun getSortKey2(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.ALPHABETICAL -> "abcasc"
			SortOrder.POPULARITY -> "favdesc"
			SortOrder.NEWEST -> "datedesc"
			else -> "favdesc"
		}
}