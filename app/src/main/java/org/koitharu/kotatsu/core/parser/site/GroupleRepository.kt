package org.koitharu.kotatsu.core.parser.site

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.util.*

abstract class GroupleRepository(loaderContext: MangaLoaderContext) :
	RemoteMangaRepository(loaderContext) {

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.RATING
	)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
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
			tag == null -> loaderContext.httpGet(
				"https://$domain/list?sortType=${
					getSortKey(
						sortOrder
					)
				}&offset=${offset upBy PAGE_SIZE}"
			)
			else -> loaderContext.httpGet(
				"https://$domain/list/genre/${tag.key}?sortType=${
					getSortKey(
						sortOrder
					)
				}&offset=${offset upBy PAGE_SIZE}"
			)
		}.parseHtml()
		val root = doc.body().getElementById("mangaBox")
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
		val doc = loaderContext.httpGet(manga.url.withDomain()).parseHtml()
		val root = doc.body().getElementById("mangaBox")?.selectFirst("div.leftContent")
			?: throw ParseException("Cannot find root")
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
				?.select("a")?.asReversed()?.mapIndexed { i, a ->
					val href = a.relUrl("href")
					MangaChapter(
						id = generateUid(href),
						name = a.ownText().removePrefix(manga.title).trim(),
						number = i + 1,
						url = href,
						source = source
					)
				}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = loaderContext.httpGet(chapter.url.withDomain() + "?mtr=1").parseHtml()
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
		val doc = loaderContext.httpGet("https://${getDomain()}/list/genres/sort_name").parseHtml()
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

	private companion object {

		private const val PAGE_SIZE = 70
		private const val PAGE_SIZE_SEARCH = 50
	}
}