package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.parseHtml
import org.koitharu.kotatsu.utils.ext.withDomain

class HenChanRepository : ChanRepository() {

	override val defaultDomain = "h-chan.me"
	override val source = MangaSource.HENCHAN

	override suspend fun getDetails(manga: Manga): Manga {
		val domain = conf.getDomain(defaultDomain)
		val doc = loaderContext.httpGet(manga.url).parseHtml()
		val root =
			doc.body().getElementById("dle-content") ?: throw ParseException("Cannot find root")
		return manga.copy(
			description = root.getElementById("description")?.html()?.substringBeforeLast("<div"),
			largeCoverUrl = root.getElementById("cover")?.attr("src")?.withDomain(domain),
			chapters = root.getElementById("right").select("table.table_cha").flatMap { table ->
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
}