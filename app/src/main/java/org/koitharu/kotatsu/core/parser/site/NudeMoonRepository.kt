package org.koitharu.kotatsu.core.parser.site

import android.util.SparseArray
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.AuthRequiredException
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.MangaRepositoryAuthProvider
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class NudeMoonRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext),
	MangaRepositoryAuthProvider {

	override val source = MangaSource.NUDEMOON
	override val defaultDomain = "nude-moon.net"
	override val authUrl: String
		get() = "https://${getDomain()}/index.php"

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.NEWEST,
		SortOrder.POPULARITY,
		SortOrder.RATING
	)

	private val pageUrlPatter = Pattern.compile(".*\\?page=[0-9]+$")

	init {
		loaderContext.cookieJar.insertCookies(
			getDomain(),
			"NMfYa=1;",
			"nm_mobile=0;"
		)
	}

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> {
		val domain = getDomain()
		val url = when {
			!query.isNullOrEmpty() -> "https://$domain/search?stext=${query.urlEncoded()}&rowstart=$offset"
			!tags.isNullOrEmpty() -> tags.joinToString(
				separator = "_",
				prefix = "https://$domain/tags/",
				postfix = "&rowstart=$offset",
				transform = { it.key.urlEncoded() }
			)
			else -> "https://$domain/all_manga?${getSortKey(sortOrder)}&rowstart=$offset"
		}
		val doc = loaderContext.httpGet(url).parseHtml()
		val root = doc.body().run {
			selectFirst("td.main-bg") ?: selectFirst("td.main-body")
		} ?: parseFailed("Cannot find root")
		return root.select("table.news_pic2").mapNotNull { row ->
			val a = row.selectFirst("td.bg_style1")?.selectFirst("a")
				?: return@mapNotNull null
			val href = a.relUrl("href")
			val title = a.selectFirst("h2")?.text().orEmpty()
			val info = row.selectFirst("td[width=100%]") ?: return@mapNotNull null
			Manga(
				id = generateUid(href),
				url = href,
				title = title.substringAfter(" / "),
				altTitle = title.substringBefore(" / ", "")
					.takeUnless { it.isBlank() },
				author = info.getElementsContainingOwnText("Автор:").firstOrNull()
					?.nextElementSibling()?.ownText(),
				coverUrl = row.selectFirst("img.news_pic2")?.absUrl("data-src")
					.orEmpty(),
				tags = row.selectFirst("span.tag-links")?.select("a")
					?.mapToSet {
						MangaTag(
							title = it.text().toTitleCase(),
							key = it.attr("href").substringAfterLast('/'),
							source = source
						)
					}.orEmpty(),
				source = source,
				publicUrl = a.absUrl("href"),
				rating = Manga.NO_RATING,
				isNsfw = true,
				description = row.selectFirst("div.description")?.html(),
				state = null,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val body = loaderContext.httpGet(manga.url.withDomain()).parseHtml().body()
		val root = body.selectFirst("table.shoutbox")
			?: parseFailed("Cannot find root")
		val info = root.select("div.tbl2")
		val lastInfo = info.last()
		return manga.copy(
			largeCoverUrl = body.selectFirst("img.news_pic2")?.absUrl("src"),
			description = info.select("div.blockquote").lastOrNull()?.html() ?: manga.description,
			tags = info.select("span.tag-links").firstOrNull()?.select("a")?.mapToSet {
				MangaTag(
					title = it.text().toTitleCase(),
					key = it.attr("href").substringAfterLast('/'),
					source = source,
				)
			}?.plus(manga.tags) ?: manga.tags,
			author = lastInfo?.getElementsByAttributeValueContaining("href", "mangaka/")?.text()
				?: manga.author,
			chapters = listOf(
				MangaChapter(
					id = manga.id,
					url = manga.url,
					source = source,
					number = 1,
					name = manga.title,
					scanlator = lastInfo?.getElementsByAttributeValueContaining("href", "perevod/")?.text(),
					uploadDate = lastInfo?.getElementsContainingOwnText("Дата:")
						?.firstOrNull()
						?.html()
						?.parseDate() ?: 0L,
					branch = null,
				)
			)
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.withDomain()
		val doc = loaderContext.httpGet(fullUrl).parseHtml()
		val root = doc.body().selectFirst("td.main-body")
			?: parseFailed("Cannot find root")
		val readlink = root.selectFirst("table.shoutbox")?.selectFirst("a")?.absUrl("href")
			?: parseFailed("Cannot obtain read link")
		val fullPages = getFullPages(readlink)
		return root.getElementsByAttributeValueMatching("href", pageUrlPatter).mapIndexedNotNull { i, a ->
			val url = a.relUrl("href")
			MangaPage(
				id = generateUid(url),
				url = fullPages[i] ?: return@mapIndexedNotNull null,
				referer = fullUrl,
				preview = a.selectFirst("img")?.absUrl("src"),
				source = source,
			)
		}
	}

	override suspend fun getTags(): Set<MangaTag> {
		val domain = getDomain()
		val doc = loaderContext.httpGet("https://$domain/all_manga").parseHtml()
		val root = doc.body().getElementsContainingOwnText("Поиск манги по тегам")
			.firstOrNull()?.parents()?.find { it.tag().normalName() == "tbody" }
			?.selectFirst("td.textbox")?.selectFirst("td.small")
			?: parseFailed("Tags root not found")
		return root.select("a").mapToSet {
			MangaTag(
				title = it.text().toTitleCase(),
				key = it.attr("href").substringAfterLast('/')
					.removeSuffix("+"),
				source = source,
			)
		}
	}

	override fun isAuthorized(): Boolean {
		return loaderContext.cookieJar.getCookies(getDomain()).any {
			it.name == "fusion_user"
		}
	}

	override suspend fun getUsername(): String {
		val body = loaderContext.httpGet("https://${getDomain()}/").parseHtml()
			.body()
		return body
			.getElementsContainingOwnText("Профиль")
			.firstOrNull()
			?.attr("href")
			?.substringAfterLast('/')
			?: run {
				throw if (body.selectFirst("form[name=\"loginform\"]") != null) {
					AuthRequiredException(source)
				} else {
					ParseException("Cannot find username")
				}
			}
	}

	private suspend fun getFullPages(url: String): SparseArray<String> {
		val scripts = loaderContext.httpGet(url).parseHtml().select("script")
		val regex = "images\\[(\\d+)].src = '([^']+)'".toRegex()
		for (script in scripts) {
			val src = script.html()
			if (src.isEmpty()) {
				continue
			}
			val matches = regex.findAll(src).toList()
			if (matches.isEmpty()) {
				continue
			}
			val result = SparseArray<String>(matches.size)
			matches.forEach { match ->
				val (index, link) = match.destructured
				result.append(index.toInt(), link)
			}
			return result
		}
		parseFailed("Cannot find pages list")
	}

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder ?: sortOrders.minByOrNull { it.ordinal }) {
			SortOrder.POPULARITY -> "views"
			SortOrder.NEWEST -> "date"
			SortOrder.RATING -> "like"
			else -> "like"
		}

	private fun String.parseDate(): Long {
		val dateString = substringBetweenFirst("Дата:", "<")?.trim() ?: return 0
		val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
		return dateFormat.tryParse(dateString)
	}
}
