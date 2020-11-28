package org.koitharu.kotatsu.core.parser.site

import androidx.collection.arraySetOf
import org.intellij.lang.annotations.Language
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.utils.ext.*
import java.util.*

class MangaTownRepository(loaderContext: MangaLoaderContext) :
	RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.MANGATOWN

	override val sortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.ALPHABETICAL,
		SortOrder.RATING,
		SortOrder.POPULARITY,
		SortOrder.UPDATED
	)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val domain = conf.getDomain(DOMAIN)
		val ssl = conf.isUseSsl(false)
		val scheme = if (ssl) "https" else "http"
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
				"$scheme://$domain/search?name=${query.urlEncoded()}"
			}
			tag != null -> "$scheme://$domain/directory/${tag.key}/$page.htm$sortKey"
			else -> "$scheme://$domain/directory/$page.htm$sortKey"
		}
		val doc = loaderContext.httpGet(url).parseHtml()
		val root = doc.body().selectFirst("ul.manga_pic_list")
			?: throw ParseException("Root not found")
		return root.select("li").mapNotNull { li ->
			val a = li.selectFirst("a.manga_cover")
			val href = a.attr("href").withDomain(domain, ssl)
			val views = li.select("p.view")
			val status = views.findOwnText { x -> x.startsWith("Status:") }
				?.substringAfter(':')?.trim()?.toLowerCase(Locale.ROOT)
			Manga(
				id = href.longHashCode(),
				title = a.attr("title"),
				coverUrl = a.selectFirst("img").attr("src"),
				source = MangaSource.MANGATOWN,
				altTitle = null,
				rating = li.selectFirst("p.score")?.selectFirst("b")
					?.ownText()?.toFloatOrNull()?.div(5f) ?: Manga.NO_RATING,
				largeCoverUrl = null,
				author = views.findText { x -> x.startsWith("Author:") }?.substringAfter(':')
					?.trim(),
				state = when (status) {
					"ongoing" -> MangaState.ONGOING
					"completed" -> MangaState.FINISHED
					else -> null
				},
				tags = li.selectFirst("p.keyWord")?.select("a")?.mapNotNullToSet tags@{ x ->
					MangaTag(
						title = x.attr("title"),
						key = x.attr("href").parseTagKey() ?: return@tags null,
						source = MangaSource.MANGATOWN
					)
				}.orEmpty(),
				url = href
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val domain = conf.getDomain(DOMAIN)
		val ssl = conf.isUseSsl(false)
		val doc = loaderContext.httpGet(manga.url).parseHtml()
		val root = doc.body().selectFirst("section.main")
			?.selectFirst("div.article_content") ?: throw ParseException("Cannot find root")
		val info = root.selectFirst("div.detail_info").selectFirst("ul")
		val chaptersList = root.selectFirst("div.chapter_content")
			?.selectFirst("ul.chapter_list")?.select("li")?.asReversed()
		return manga.copy(
			tags = manga.tags + info.select("li").find { x ->
				x.selectFirst("b")?.ownText() == "Genre(s):"
			}?.select("a")?.mapNotNull { a ->
				MangaTag(
					title = a.attr("title"),
					key = a.attr("href").parseTagKey() ?: return@mapNotNull null,
					source = MangaSource.MANGATOWN
				)
			}.orEmpty(),
			description = info.getElementById("show")?.ownText(),
			chapters = chaptersList?.mapIndexedNotNull { i, li ->
				val href = li.selectFirst("a").attr("href").withDomain(domain, ssl)
				val name = li.select("span").filter { it.className().isEmpty() }
					.joinToString(" - ") { it.text() }.trim()
				MangaChapter(
					id = href.longHashCode(),
					url = href,
					source = MangaSource.MANGATOWN,
					number = i + 1,
					name = if (name.isEmpty()) "${manga.title} - ${i + 1}" else name
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val domain = conf.getDomain(DOMAIN)
		val ssl = conf.isUseSsl(false)
		val doc = loaderContext.httpGet(chapter.url).parseHtml()
		val root = doc.body().selectFirst("div.page_select")
			?: throw ParseException("Cannot find root")
		return root.selectFirst("select").select("option").mapNotNull {
			val href = it.attr("value").withDomain(domain, ssl)
			if (href.endsWith("featured.html")) {
				return@mapNotNull null
			}
			MangaPage(
				id = href.longHashCode(),
				url = href,
				source = MangaSource.MANGATOWN
			)
		}
	}

	override suspend fun getPageFullUrl(page: MangaPage): String {
		val domain = conf.getDomain(DOMAIN)
		val ssl = conf.isUseSsl(false)
		val doc = loaderContext.httpGet(page.url).parseHtml()
		return doc.getElementById("image").attr("src").withDomain(domain, ssl)
	}

	override suspend fun getTags(): Set<MangaTag> {
		val domain = conf.getDomain(DOMAIN)
		val doc = loaderContext.httpGet("http://$domain/directory/").parseHtml()
		val root = doc.body().selectFirst("aside.right")
			.getElementsContainingOwnText("Genres")
			.first()
			.nextElementSibling()
		return root.select("li").mapNotNullToSet { li ->
			val a = li.selectFirst("a") ?: return@mapNotNullToSet null
			val key = a.attr("href").parseTagKey()
			if (key.isNullOrEmpty()) {
				return@mapNotNullToSet null
			}
			MangaTag(
				source = MangaSource.MANGATOWN,
				key = key,
				title = a.text()
			)
		}
	}


	override fun onCreatePreferences() = arraySetOf(
		SourceSettings.KEY_DOMAIN,
		SourceSettings.KEY_USE_SSL
	)

	private fun String.parseTagKey() = split('/').findLast { TAG_REGEX matches it }

	private companion object {

		@Language("RegExp")
		val TAG_REGEX = Regex("[^\\-]+-[^\\-]+-[^\\-]+-[^\\-]+-[^\\-]+-[^\\-]+")
		const val DOMAIN = "www.mangatown.com"
	}
}