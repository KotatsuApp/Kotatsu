package org.koitharu.kotatsu.domain.repository

import androidx.core.text.parseAsHtml
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.domain.BaseMangaRepository
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.domain.exceptions.ParseException
import org.koitharu.kotatsu.utils.ext.*

class ReadmangaRepository(loaderContext: MangaLoaderContext) : BaseMangaRepository(loaderContext) {

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tags: Set<String>?
	): List<Manga> {
		val doc = loaderContext.get("https://readmanga.me/list?sortType=updated&offset=$offset")
			.parseHtml()
		val root = doc.body().getElementById("mangaBox")
			?.selectFirst("div.tiles.row") ?: throw ParseException("Cannot find root")
		return root.select("div.tile").mapNotNull { node ->
			val imgDiv = node.selectFirst("div.img") ?: return@mapNotNull null
			val descDiv = node.selectFirst("div.desc") ?: return@mapNotNull null
			val href = imgDiv.selectFirst("a").attr("href")?.withDomain("readmanga.me")
				?: return@mapNotNull null
			val title = descDiv.selectFirst("h3")?.selectFirst("a")?.text()
				?: return@mapNotNull null
			Manga(
				id = href.longHashCode(),
				url = href,
				localizedTitle = title,
				title = descDiv.selectFirst("h4")?.text() ?: title,
				coverUrl = imgDiv.selectFirst("img.lazy")?.attr("data-original").orEmpty(),
				summary = "",
				rating = safe {
					node.selectFirst("div.rating")
						?.attr("title")
						?.substringBefore(' ')
						?.toFloatOrNull()
						?.div(10f)
				} ?: Manga.NO_RATING,
				tags = safe {
					descDiv.selectFirst("div.tile-info")
						?.select("a.element-link")
						?.map {
							MangaTag(
								title = it.text(),
								key = it.attr("href").substringAfterLast('/'),
								source = MangaSource.READMANGA_RU
							)
						}?.toSet()
				}.orEmpty(),
				state = when {
					node.selectFirst("div.tags")
						?.selectFirst("span.mangaCompleted") != null -> MangaState.FINISHED
					else -> null
				},
				source = MangaSource.READMANGA_RU
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.get(manga.url).parseHtml()
		val root = doc.body().getElementById("mangaBox")
		return manga.copy(
			description = root.selectFirst("div.manga-description").firstChild()?.html()?.parseAsHtml(),
			largeCoverUrl = root.selectFirst("div.subject-cower")?.selectFirst("img")?.attr(
				"data-full"
			),
			chapters = root.selectFirst("div.chapters-link")?.selectFirst("table")
				?.select("a")?.asReversed()?.mapIndexedNotNull { i, a ->
					val href =
						a.attr("href")?.withDomain("readmanga.me") ?: return@mapIndexedNotNull null
					MangaChapter(
						id = href.longHashCode(),
						name = a.ownText(),
						number = i + 1,
						url = href,
						source = MangaSource.READMANGA_RU
					)
				}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = loaderContext.get(chapter.url).parseHtml()
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
				val url = parts[1].value.removeSurrounding('"', '\'') +
						parts[2].value.removeSurrounding('"', '\'')
				MangaPage(
					id = url.longHashCode(),
					url = url,
					source = MangaSource.READMANGA_RU
				)
			}
		}
		throw ParseException("Pages list not found at ${chapter.url}")
	}
}