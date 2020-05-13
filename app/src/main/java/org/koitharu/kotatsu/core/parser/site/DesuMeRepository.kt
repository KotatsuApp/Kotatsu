package org.koitharu.kotatsu.core.parser.site

import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.ParseException
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.domain.MangaLoaderContext
import org.koitharu.kotatsu.utils.ext.map
import org.koitharu.kotatsu.utils.ext.mapIndexed
import org.koitharu.kotatsu.utils.ext.parseHtml
import org.koitharu.kotatsu.utils.ext.parseJson

class DesuMeRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.DESUME

	override val sortOrders = setOf(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL
	)

	override suspend fun getList(
		offset: Int,
		query: String?,
		sortOrder: SortOrder?,
		tag: MangaTag?
	): List<Manga> {
		val domain = conf.getDomain(DOMAIN)
		val url = buildString {
			append("https://")
			append(domain)
			append("/manga/api/?limit=20&order=")
			append(getSortKey(sortOrder))
			append("&page=")
			append((offset / 20) + 1)
			if (tag != null) {
				append("&genres=")
				append(tag.key)
			}
			if (query != null) {
				append("&search=")
				append(query)
			}
		}
		val json = loaderContext.httpGet(url).parseJson().getJSONArray("response")
			?: throw ParseException("Invalid response")
		val total = json.length()
		val list = ArrayList<Manga>(total)
		for (i in 0 until total) {
			val jo = json.getJSONObject(i)
			val cover = jo.getJSONObject("image")
			list += Manga(
				url = jo.getString("url"),
				source = MangaSource.DESUME,
				title = jo.getString("russian"),
				altTitle = jo.getString("name"),
				coverUrl = cover.getString("preview"),
				largeCoverUrl = cover.getString("original"),
				state = when {
					jo.getInt("ongoing") == 1 -> MangaState.ONGOING
					else -> null
				},
				rating = jo.getDouble("score").toFloat().coerceIn(0f, 1f),
				id = ID_MASK + jo.getLong("id"),
				description = jo.getString("description")
			)
		}
		return list
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val domain = conf.getDomain(DOMAIN)
		val url = "https://$domain/manga/api/${manga.id - ID_MASK}"
		val json = loaderContext.httpGet(url).parseJson().getJSONObject("response")
			?: throw ParseException("Invalid response")
		return manga.copy(
			tags = json.getJSONArray("genres").map {
				MangaTag(
					key = it.getString("text"),
					title = it.getString("russian"),
					source = manga.source
				)
			}.toSet(),
			description = json.getString("description"),
			chapters = json.getJSONObject("chapters").getJSONArray("list").mapIndexed { i, it ->
				val chid = it.getLong("id")
				MangaChapter(
					id = ID_MASK + chid,
					source = manga.source,
					url = "$url/chapter/$chid",
					name = it.optString("title", "${manga.title} #${it.getDouble("ch")}"),
					number = i + 1
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val json = loaderContext.httpGet(chapter.url).parseJson().getJSONObject("response")
			?: throw ParseException("Invalid response")
		return json.getJSONObject("pages").getJSONArray("list").map {
			MangaPage(
				id = it.getLong("id"),
				source = chapter.source,
				url = it.getString("img")
			)
		}
	}

	override suspend fun getTags(): Set<MangaTag> {
		val domain = conf.getDomain(DOMAIN)
		val doc = loaderContext.httpGet("https://$domain/manga/").parseHtml()
		val root = doc.body().getElementById("animeFilter").selectFirst(".catalog-genres")
		return root.select("li").map {
			MangaTag(
				source = source,
				key = it.selectFirst("input").attr("data-genre"),
				title = it.selectFirst("label").text()
			)
		}.toSet()
	}

	override fun onCreatePreferences() = setOf(R.string.key_parser_domain)

	private fun getSortKey(sortOrder: SortOrder?) =
		when (sortOrder) {
			SortOrder.ALPHABETICAL -> "name"
			SortOrder.POPULARITY -> "popular"
			SortOrder.UPDATED -> "updated"
			SortOrder.NEWEST -> "id"
			else -> "updated"
		}

	private companion object {

		private const val ID_MASK = 1000
		private const val DOMAIN = "desu.me"
	}
}