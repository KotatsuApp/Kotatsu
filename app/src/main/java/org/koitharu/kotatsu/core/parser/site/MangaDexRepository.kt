package org.koitharu.kotatsu.core.parser.site

import android.os.Build
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import org.koitharu.kotatsu.base.domain.MangaLoaderContext
import org.koitharu.kotatsu.core.model.*
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.*
import java.text.SimpleDateFormat
import java.util.*

private const val PAGE_SIZE = 20
private const val CONTENT_RATING =
	"contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic"
private const val LOCALE_FALLBACK = "en"

class MangaDexRepository(loaderContext: MangaLoaderContext) : RemoteMangaRepository(loaderContext) {

	override val source = MangaSource.MANGADEX
	override val defaultDomain = "mangadex.org"

	override val sortOrders: EnumSet<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.ALPHABETICAL,
		SortOrder.NEWEST,
		SortOrder.POPULARITY,
	)

	override suspend fun getList2(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?,
	): List<Manga> {
		val domain = getDomain()
		val url = buildString {
			append("https://api.")
			append(domain)
			append("/manga?limit=")
			append(PAGE_SIZE)
			append("&offset=")
			append(offset)
			append("&includes[]=cover_art&includes[]=author&includes[]=artist&")
			tags?.forEach { tag ->
				append("includedTags[]=")
				append(tag.key)
				append('&')
			}
			if (!query.isNullOrEmpty()) {
				append("title=")
				append(query.urlEncoded())
				append('&')
			}
			append(CONTENT_RATING)
			append("&order")
			append(when (sortOrder) {
				null,
				SortOrder.UPDATED,
				-> "[latestUploadedChapter]=desc"
				SortOrder.ALPHABETICAL -> "[title]=asc"
				SortOrder.NEWEST -> "[createdAt]=desc"
				SortOrder.POPULARITY -> "[followedCount]=desc"
				else -> "[followedCount]=desc"
			})
		}
		val json = loaderContext.httpGet(url).parseJson().getJSONArray("data")
		return json.map { jo ->
			val id = jo.getString("id")
			val attrs = jo.getJSONObject("attributes")
			val relations = jo.getJSONArray("relationships").associateByKey("type")
			val cover = relations["cover_art"]
				?.getJSONObject("attributes")
				?.getString("fileName")
				?.let {
					"https://uploads.$domain/covers/$id/$it"
				}
			Manga(
				id = generateUid(id),
				title = requireNotNull(attrs.getJSONObject("title").selectByLocale()) {
					"Title should not be null"
				},
				altTitle = attrs.optJSONObject("altTitles")?.selectByLocale(),
				url = id,
				publicUrl = "https://$domain/title/$id",
				rating = Manga.NO_RATING,
				isNsfw = attrs.getStringOrNull("contentRating") == "erotica",
				coverUrl = cover?.plus(".256.jpg").orEmpty(),
				largeCoverUrl = cover,
				description = attrs.optJSONObject("description")?.selectByLocale(),
				tags = attrs.getJSONArray("tags").mapToSet { tag ->
					MangaTag(
						title = tag.getJSONObject("attributes")
							.getJSONObject("name")
							.firstStringValue(),
						key = tag.getString("id"),
						source = source,
					)
				},
				state = when (jo.getStringOrNull("status")) {
					"ongoing" -> MangaState.ONGOING
					"completed" -> MangaState.FINISHED
					else -> null
				},
				author = (relations["author"] ?: relations["artist"])
					?.getJSONObject("attributes")
					?.getStringOrNull("name"),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope<Manga> {
		val domain = getDomain()
		val attrsDeferred = async {
			loaderContext.httpGet(
				"https://api.$domain/manga/${manga.url}?includes[]=artist&includes[]=author&includes[]=cover_art"
			).parseJson().getJSONObject("data").getJSONObject("attributes")
		}
		val feedDeferred = async {
			val url = buildString {
				append("https://api.")
				append(domain)
				append("/manga/")
				append(manga.url)
				append("/feed")
				append("?limit=96&includes[]=scanlation_group&order[volume]=asc&order[chapter]=asc&offset=0&")
				append(CONTENT_RATING)
			}
			loaderContext.httpGet(url).parseJson().getJSONArray("data")
		}
		val mangaAttrs = attrsDeferred.await()
		val feed = feedDeferred.await()
		//2022-01-02T00:27:11+00:00
		val dateFormat = SimpleDateFormat(
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				"yyyy-MM-dd'T'HH:mm:ssX"
			} else {
				"yyyy-MM-dd'T'HH:mm:ss'+00:00'"
			},
			Locale.ROOT
		)
		manga.copy(
			description = mangaAttrs.getJSONObject("description").selectByLocale()
				?: manga.description,
			chapters = feed.mapNotNull { jo ->
				val id = jo.getString("id")
				val attrs = jo.getJSONObject("attributes")
				if (!attrs.isNull("externalUrl")) {
					return@mapNotNull null
				}
				val locale = Locale.forLanguageTag(attrs.getString("translatedLanguage"))
				val relations = jo.getJSONArray("relationships").associateByKey("type")
				val number = attrs.optInt("chapter", 0)
				MangaChapter(
					id = generateUid(id),
					name = attrs.getStringOrNull("title")?.takeUnless(String::isEmpty)
						?: "Chapter #$number",
					number = number,
					url = id,
					scanlator = relations["scanlation_group"]?.getStringOrNull("name"),
					uploadDate = dateFormat.tryParse(attrs.getString("publishAt")),
					branch = locale.displayName.toTitleCase(locale),
					source = source,
				)
			}
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val domain = getDomain()
		val attrs = loaderContext.httpGet("https://api.$domain/chapter/${chapter.url}")
			.parseJson()
			.getJSONObject("data")
			.getJSONObject("attributes")
		val pages = attrs.getJSONArray("pages")
		val prefix = "https://uploads.$domain/data/${attrs.getString("hash")}/"
		val referer = "https://$domain/"
		return List(pages.length()) { i ->
			val url = prefix + pages.getString(i)
			MangaPage(
				id = generateUid(url),
				url = url,
				referer = referer,
				preview = null, // TODO prefix + dataSaver.getString(i),
				source = source,
			)
		}
	}

	override suspend fun getTags(): Set<MangaTag> {
		val tags = loaderContext.httpGet("https://api.${getDomain()}/manga/tag").parseJson()
			.getJSONArray("data")
		return tags.mapToSet { jo ->
			MangaTag(
				title = jo.getJSONObject("attributes").getJSONObject("name").firstStringValue(),
				key = jo.getString("id"),
				source = source,
			)
		}
	}

	private fun JSONObject.firstStringValue() = values().next() as String

	private fun JSONObject.selectByLocale(): String? {
		val preferredLocales = LocaleListCompat.getAdjustedDefault()
		repeat(preferredLocales.size()) { i ->
			val locale = preferredLocales.get(i)
			getStringOrNull(locale.language)?.let { return it }
			getStringOrNull(locale.toLanguageTag())?.let { return it }
		}
		return getStringOrNull(LOCALE_FALLBACK) ?: values().nextOrNull() as? String
	}
}