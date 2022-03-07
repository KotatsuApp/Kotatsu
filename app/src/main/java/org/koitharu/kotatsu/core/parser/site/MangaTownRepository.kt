package org.koitharu.kotatsu.core.parser.site

import org.intellij.lang.annotations.Language
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.utils.ext.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MangaTownRepository(loaderContext: MangaLoaderContext) :
	RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.MANGATOWN

	override val defaultDomain = "www.mangatown.com"

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.ALPHABETICAL,
		SortOrder.RATING,
		SortOrder.POPULARITY,
		SortOrder.UPDATED
	)

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		val sortKey = when (sortOrder) {
			SortOrder.ALPHABETICAL -> "?name.az"
			SortOrder.RATING -> "?rating.za"
			SortOrder.UPDATED -> "?last_chapter_time.za"
			else -> ""
		}
		val page = (offset / 30) + 1
		val url = when {
			!query.isNullOrEmpty() -> {
				if (offset != 0) {
					return emptyList()
				}
				"/search?name=${query.urlEncoded()}".withDomain()
			}
			tags.isNullOrEmpty() -> "/directory/$page.htm$sortKey".withDomain()
			tags.size == 1 -> "/directory/${tags.first().key}/$page.htm$sortKey".withDomain()
			else -> tags.joinToString(
				prefix = "/search?page=$page".withDomain()
			) { tag ->
				"&genres[${tag.key}]=1"
			}
		}
		val doc = loaderContext.httpGet(url).parseHtml()
		val root = doc.body().selectFirst("ul.manga_pic_list")
			?: throw ParseException("Root not found")
		return root.select("li").mapNotNull { li ->
			val a = li.selectFirst("a.manga_cover")
			val href = a?.relUrl("href")
				?: return@mapNotNull null
			val views = li.select("p.view")
			val status = views.findOwnText { x -> x.startsWith("Status:") }
				?.substringAfter(':')?.trim()?.lowercase(Locale.ROOT)
			Manga(
				id = generateUid(href),
				title = a.attr("title"),
				coverUrl = a.selectFirst("img")?.absUrl("src").orEmpty(),
				source = MangaSource.MANGATOWN,
				altTitle = null,
				rating = li.selectFirst("p.score")?.selectFirst("b")
					?.ownText()?.toFloatOrNull()?.div(5f) ?: Manga.NO_RATING,
				author = views.findText { x -> x.startsWith("Author:") }?.substringAfter(':')
					?.trim(),
				state = when (status) {
					"ongoing" -> MangaState.ONGOING
					"completed" -> MangaState.FINISHED
					else -> null
				},
				tags = li.selectFirst("p.keyWord")?.select("a")?.mapNotNullToSet tags@{ x ->
					MangaTag(
						title = x.attr("title").toTitleCase(),
						key = x.attr("href").parseTagKey() ?: return@tags null,
						source = MangaSource.MANGATOWN
					)
				}.orEmpty(),
				url = href,
				publicUrl = href.inContextOf(a)
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = loaderContext.httpGet(manga.url.withDomain()).parseHtml()
		val root = doc.body().selectFirst("section.main")
			?.selectFirst("div.article_content") ?: throw ParseException("Cannot find root")
		val info = root.selectFirst("div.detail_info")?.selectFirst("ul")
		val chaptersList = root.selectFirst("div.chapter_content")
			?.selectFirst("ul.chapter_list")?.select("li")?.asReversed()
		val dateFormat = SimpleDateFormat("MMM dd,yyyy", Locale.US)
		return manga.copy(
			tags = manga.tags + info?.select("li")?.find { x ->
				x.selectFirst("b")?.ownText() == "Genre(s):"
			}?.select("a")?.mapNotNull { a ->
				MangaTag(
					title = a.attr("title").toTitleCase(),
					key = a.attr("href").parseTagKey() ?: return@mapNotNull null,
					source = MangaSource.MANGATOWN
				)
			}.orEmpty(),
			description = info?.getElementById("show")?.ownText(),
			chapters = chaptersList?.mapIndexedNotNull { i, li ->
				val href = li.selectFirst("a")?.relUrl("href")
					?: return@mapIndexedNotNull null
				val name = li.select("span").filter { it.className().isEmpty() }
					.joinToString(" - ") { it.text() }.trim()
				MangaChapter(
					id = generateUid(href),
					url = href,
					source = MangaSource.MANGATOWN,
					number = i + 1,
					uploadDate = parseChapterDate(
						dateFormat,
						li.selectFirst("span.time")?.text()
					),
					name = name.ifEmpty { "${manga.title} - ${i + 1}" },
					scanlator = null,
					branch = null,
				)
			} ?: bypassLicensedChapters(manga)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		val root = doc.body().selectFirst("div.page_select")
			?: throw ParseException("Cannot find root")
		return root.selectFirst("select")?.select("option")?.mapNotNull {
			val href = it.relUrl("value")
			if (href.endsWith("featured.html")) {
				return@mapNotNull null
			}
			MangaPage(
				id = generateUid(href),
				url = href,
				preview = null,
				referer = fullUrl,
				source = MangaSource.MANGATOWN,
			)
		} ?: parseFailed("Pages list not found")
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val doc = loaderContext.httpGet(page.url.withDomain()).parseHtml()
		return doc.getElementById("image")?.absUrl("src") ?: parseFailed("Image not found")
	}

	override suspend fun getTags(): Set<MangaTag> {
		val doc = loaderContext.httpGet("/directory/".withDomain()).parseHtml()
		val root = doc.body().selectFirst("aside.right")
			?.getElementsContainingOwnText("Genres")
			?.first()
			?.nextElementSibling() ?: parseFailed("Root not found")
		return root.select("li").mapNotNullToSet { li ->
			val a = li.selectFirst("a") ?: return@mapNotNullToSet null
			val key = a.attr("href").parseTagKey()
			if (key.isNullOrEmpty()) {
				return@mapNotNullToSet null
			}
			MangaTag(
				source = MangaSource.MANGATOWN,
				key = key,
				title = a.text().toTitleCase()
			)
		}
	}

	private fun parseChapterDate(dateFormat: DateFormat, date: String?): Long {
		return when {
			date.isNullOrEmpty() -> 0L
			date.contains("Today") -> Calendar.getInstance().timeInMillis
			date.contains("Yesterday") -> Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.timeInMillis
			else -> dateFormat.tryParse(date)
		}
	}

	override fun onCreatePreferences(map: MutableMap<String, Any>) {
		super.onCreatePreferences(map)
		map[SourceSettings.KEY_USE_SSL] = true
	}

	private suspend fun bypassLicensedChapters(manga: Manga): List<MangaChapter> {
		val doc = loaderContext.httpGet(manga.url.withDomain("m")).parseHtml()
		val list = doc.body().selectFirst("ul.detail-ch-list") ?: return emptyList()
		val dateFormat = SimpleDateFormat("MMM dd,yyyy", Locale.US)
		return list.select("li").asReversed().mapIndexedNotNull { i, li ->
			val a = li.selectFirst("a") ?: return@mapIndexedNotNull null
			val href = a.relUrl("href")
			val name = a.selectFirst("span.vol")?.text().orEmpty().ifEmpty {
				a.ownText()
			}
			MangaChapter(
				id = generateUid(href),
				url = href,
				source = MangaSource.MANGATOWN,
				number = i + 1,
				uploadDate = parseChapterDate(
					dateFormat,
					li.selectFirst("span.time")?.text()
				),
				name = name.ifEmpty { "${manga.title} - ${i + 1}" },
				scanlator = null,
				branch = null,
			)
		}
	}

	private fun String.parseTagKey() = split('/').findLast { TAG_REGEX matches it }

	private companion object {

		@Language("RegExp")
		val TAG_REGEX = Regex("[^\\-]+-[^\\-]+-[^\\-]+-[^\\-]+-[^\\-]+-[^\\-]+")
	}
}