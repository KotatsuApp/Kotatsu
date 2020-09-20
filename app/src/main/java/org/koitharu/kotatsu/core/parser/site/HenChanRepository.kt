package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.utils.ext.longHashCode
import org.koitharu.kotatsu.utils.ext.parseHtml
import org.koitharu.kotatsu.utils.ext.withDomain

class HenChanRepository(loaderContext: MangaLoaderContext) : ChanRepository(loaderContext) {

	override val defaultDomain = "hentaichan.pro"
	override val source = MangaSource.HENCHAN

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		return super.getList(offset, query, sortOrder, tag).map {
			val cover = it.coverUrl
			if (cover.contains("_blur")) {
				it.copy(coverUrl = cover.replace("_blur", ""))
			} else {
				it
			}
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val domain = conf.getDomain(defaultDomain)
		val doc = loaderContext.httpGet(manga.url).parseHtml()
		val root =
			doc.body().getElementById("dle-content") ?: throw ParseException("Cannot find root")
		val readLink = manga.url.replace("manga", "online")
		return manga.copy(
			description = root.getElementById("description")?.html()?.substringBeforeLast("<div"),
			largeCoverUrl = root.getElementById("cover")?.attr("src")?.withDomain(domain),
			tags = root.selectFirst("div.sidetags")?.select("li.sidetag")?.map {
				val a = it.children().last()
				MangaTag(
					title = a.text(),
					key = a.attr("href").substringAfterLast('/'),
					source = source
				)
			}?.toSet() ?: manga.tags,
			chapters = listOf(
				MangaChapter(
					id = readLink.longHashCode(),
					url = readLink,
					source = source,
					number = 1,
					name = manga.title
				)
			)
		)
	}
}