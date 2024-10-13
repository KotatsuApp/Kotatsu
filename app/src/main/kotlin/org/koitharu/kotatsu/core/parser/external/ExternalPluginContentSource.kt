package org.koitharu.kotatsu.core.parser.external

import android.content.ContentResolver
import android.database.Cursor
import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import androidx.core.net.toUri
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.exceptions.IncompatiblePluginException
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Demographic
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.find
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.splitTwoParts
import java.util.EnumSet
import java.util.Locale

class ExternalPluginContentSource(
	private val contentResolver: ContentResolver,
	private val source: ExternalMangaSource,
) {

	@Blocking
	@WorkerThread
	fun getListFilterOptions() = MangaListFilterOptions(
		availableTags = fetchTags(),
		availableStates = fetchEnumSet(MangaState::class.java, "filter/states"),
		availableContentRating = fetchEnumSet(ContentRating::class.java, "filter/content_ratings"),
		availableContentTypes = fetchEnumSet(ContentType::class.java, "filter/content_types"),
		availableDemographics = fetchEnumSet(Demographic::class.java, "filter/demographics"),
		availableLocales = fetchLocales(),
	)

	@Blocking
	@WorkerThread
	fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val uri = "content://${source.authority}/manga".toUri().buildUpon()
		uri.appendQueryParameter("offset", offset.toString())
		filter.tags.forEach { uri.appendQueryParameter("tags_include", "${it.key}=${it.title}") }
		filter.tagsExclude.forEach { uri.appendQueryParameter("tags_exclude", "${it.key}=${it.title}") }
		filter.states.forEach { uri.appendQueryParameter("state", it.name) }
		filter.locale?.let { uri.appendQueryParameter("locale", it.language) }
		filter.contentRating.forEach { uri.appendQueryParameter("content_rating", it.name) }
		if (!filter.query.isNullOrEmpty()) {
			uri.appendQueryParameter("query", filter.query)
		}
		return contentResolver.query(uri.build(), null, null, null, order.name)
			.safe()
			.use { cursor ->
				val result = ArrayList<Manga>(cursor.count)
				if (cursor.moveToFirst()) {
					do {
						result += cursor.getManga()
					} while (cursor.moveToNext())
				}
				result
			}
	}

	@Blocking
	@WorkerThread
	fun getDetails(manga: Manga): Manga {
		val chapters = queryChapters(manga.url)
		val details = queryDetails(manga.url)
		return Manga(
			id = manga.id,
			title = details.title.ifBlank { manga.title },
			altTitle = details.altTitle.ifNullOrEmpty { manga.altTitle },
			url = details.url.ifEmpty { manga.url },
			publicUrl = details.publicUrl.ifEmpty { manga.publicUrl },
			rating = maxOf(details.rating, manga.rating),
			isNsfw = details.isNsfw,
			coverUrl = details.coverUrl.ifEmpty { manga.coverUrl },
			tags = details.tags + manga.tags,
			state = details.state ?: manga.state,
			author = details.author.ifNullOrEmpty { manga.author },
			largeCoverUrl = details.largeCoverUrl.ifNullOrEmpty { manga.largeCoverUrl },
			description = details.description.ifNullOrEmpty { manga.description },
			chapters = chapters,
			source = source,
		)
	}

	@Blocking
	@WorkerThread
	fun getPages(chapter: MangaChapter): List<MangaPage> {
		val uri = "content://${source.authority}/chapters".toUri()
			.buildUpon()
			.appendPath(chapter.url)
			.build()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				val result = ArrayList<MangaPage>(cursor.count)
				if (cursor.moveToFirst()) {
					do {
						result += MangaPage(
							id = cursor.getLong(COLUMN_ID),
							url = cursor.getString(COLUMN_URL),
							preview = cursor.getStringOrNull(COLUMN_PREVIEW),
							source = source,
						)
					} while (cursor.moveToNext())
				}
				result
			}
	}

	@Blocking
	@WorkerThread
	private fun fetchTags(): Set<MangaTag> {
		val uri = "content://${source.authority}/filter/tags".toUri()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				val result = ArraySet<MangaTag>(cursor.count)
				if (cursor.moveToFirst()) {
					do {
						result += MangaTag(
							key = cursor.getString(COLUMN_KEY),
							title = cursor.getString(COLUMN_TITLE),
							source = source,
						)
					} while (cursor.moveToNext())
				}
				result
			}
	}

	@Blocking
	@WorkerThread
	fun getPageUrl(url: String): String {
		val uri = "content://${source.authority}/manga/pages/0".toUri().buildUpon()
			.appendQueryParameter("url", url)
			.build()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				if (cursor.moveToFirst()) {
					cursor.getString(COLUMN_VALUE)
				} else {
					url
				}
			}
	}

	@Blocking
	@WorkerThread
	private fun fetchLocales(): Set<Locale> {
		val uri = "content://${source.authority}/filter/locales".toUri()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				val result = ArraySet<Locale>(cursor.count)
				if (cursor.moveToFirst()) {
					do {
						result += Locale(cursor.getString(COLUMN_NAME))
					} while (cursor.moveToNext())
				}
				result
			}
	}

	fun getCapabilities(): MangaSourceCapabilities? {
		val uri = "content://${source.authority}/capabilities".toUri()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				if (cursor.moveToFirst()) {
					MangaSourceCapabilities(
						availableSortOrders = cursor.getStringOrNull(COLUMN_SORT_ORDERS)
							?.split(',')
							?.mapNotNullTo(EnumSet.noneOf(SortOrder::class.java)) {
								SortOrder.entries.find(it)
							}.orEmpty(),
						listFilterCapabilities = MangaListFilterCapabilities(
							isMultipleTagsSupported = cursor.getBooleanOrDefault(COLUMN_MULTIPLE_TAGS, false),
							isTagsExclusionSupported = cursor.getBooleanOrDefault(COLUMN_TAGS_EXCLUSION, false),
							isSearchSupported = cursor.getBooleanOrDefault(COLUMN_SEARCH, false),
							isSearchWithFiltersSupported = cursor.getBooleanOrDefault(
								COLUMN_SEARCH_WITH_FILTERS,
								false,
							),
							isYearSupported = cursor.getBooleanOrDefault(COLUMN_YEAR, false),
							isYearRangeSupported = cursor.getBooleanOrDefault(COLUMN_YEAR_RANGE, false),
							isOriginalLocaleSupported = cursor.getBooleanOrDefault(COLUMN_ORIGINAL_LOCALE, false),
						),
					)
				} else {
					null
				}
			}
	}

	private fun queryDetails(url: String): Manga {
		val uri = "content://${source.authority}/manga".toUri()
			.buildUpon()
			.appendPath(url)
			.build()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				cursor.moveToFirst()
				cursor.getManga()
			}
	}

	private fun queryChapters(url: String): List<MangaChapter> {
		val uri = "content://${source.authority}/manga/chapters".toUri()
			.buildUpon()
			.appendPath(url)
			.build()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				val result = ArrayList<MangaChapter>(cursor.count)
				if (cursor.moveToFirst()) {
					do {
						result += MangaChapter(
							id = cursor.getLong(COLUMN_ID),
							name = cursor.getString(COLUMN_NAME),
							number = cursor.getFloatOrDefault(COLUMN_NUMBER, 0f),
							volume = cursor.getIntOrDefault(COLUMN_VOLUME, 0),
							url = cursor.getString(COLUMN_URL),
							scanlator = cursor.getStringOrNull(COLUMN_SCANLATOR),
							uploadDate = cursor.getLongOrDefault(COLUMN_UPLOAD_DATE, 0L),
							branch = cursor.getStringOrNull(COLUMN_BRANCH),
							source = source,
						)
					} while (cursor.moveToNext())
				}
				result
			}
	}

	private fun ExternalPluginCursor.getManga() = Manga(
		id = getLong(COLUMN_ID),
		title = getString(COLUMN_TITLE),
		altTitle = getStringOrNull(COLUMN_ALT_TITLE),
		url = getString(COLUMN_URL),
		publicUrl = getString(COLUMN_PUBLIC_URL),
		rating = getFloat(COLUMN_RATING),
		isNsfw = getBooleanOrDefault(COLUMN_IS_NSFW, false),
		coverUrl = getString(COLUMN_COVER_URL),
		tags = getStringOrNull(COLUMN_TAGS)?.split(':')?.mapNotNullToSet {
			val parts = it.splitTwoParts('=') ?: return@mapNotNullToSet null
			MangaTag(key = parts.first, title = parts.second, source = source)
		}.orEmpty(),
		state = getStringOrNull(COLUMN_STATE)?.let { MangaState.entries.find(it) },
		author = getStringOrNull(COLUMN_AUTHOR),
		largeCoverUrl = getStringOrNull(COLUMN_LARGE_COVER_URL),
		description = getStringOrNull(COLUMN_DESCRIPTION),
		chapters = emptyList(),
		source = source,
	)

	private fun <E : Enum<E>> fetchEnumSet(cls: Class<E>, path: String): EnumSet<E> {
		val uri = "content://${source.authority}/$path".toUri()
		return contentResolver.query(uri, null, null, null, null)
			.safe()
			.use { cursor ->
				val result = EnumSet.noneOf(cls)
				val enumConstants = cls.enumConstants ?: return@use result
				if (cursor.moveToFirst()) {
					do {
						val name = cursor.getString(COLUMN_NAME)
						val enumValue = enumConstants.find { it.name == name }
						if (enumValue != null) {
							result.add(enumValue)
						}
					} while (cursor.moveToNext())
				}
				result
			}
	}

	private fun Cursor?.safe() = ExternalPluginCursor(
		source = source,
		cursor = this ?: throw IncompatiblePluginException(source.name, null),
	)

	class MangaSourceCapabilities(
		val availableSortOrders: Set<SortOrder>,
		val listFilterCapabilities: MangaListFilterCapabilities,
	)

	private companion object {

		const val COLUMN_SORT_ORDERS = "sort_orders"
		const val COLUMN_MULTIPLE_TAGS = "multiple_tags"
		const val COLUMN_TAGS_EXCLUSION = "tags_exclusion"
		const val COLUMN_SEARCH = "search"
		const val COLUMN_SEARCH_WITH_FILTERS = "search_with_filters"
		const val COLUMN_YEAR = "year"
		const val COLUMN_YEAR_RANGE = "year_range"
		const val COLUMN_ORIGINAL_LOCALE = "original_locale"
		const val COLUMN_ID = "id"
		const val COLUMN_NAME = "name"
		const val COLUMN_NUMBER = "number"
		const val COLUMN_VOLUME = "volume"
		const val COLUMN_URL = "url"
		const val COLUMN_SCANLATOR = "scanlator"
		const val COLUMN_UPLOAD_DATE = "upload_date"
		const val COLUMN_BRANCH = "branch"
		const val COLUMN_TITLE = "title"
		const val COLUMN_ALT_TITLE = "alt_title"
		const val COLUMN_PUBLIC_URL = "public_url"
		const val COLUMN_RATING = "rating"
		const val COLUMN_IS_NSFW = "is_nsfw"
		const val COLUMN_COVER_URL = "cover_url"
		const val COLUMN_TAGS = "tags"
		const val COLUMN_STATE = "state"
		const val COLUMN_AUTHOR = "author"
		const val COLUMN_LARGE_COVER_URL = "large_cover_url"
		const val COLUMN_DESCRIPTION = "description"
		const val COLUMN_PREVIEW = "preview"
		const val COLUMN_KEY = "key"
		const val COLUMN_VALUE = "value"
	}
}
