package org.koitharu.kotatsu.core.parser.external

import android.content.ContentResolver
import android.database.Cursor
import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import androidx.core.net.toUri
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.exceptions.IncompatiblePluginException
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.toLocale
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
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
	fun getList(offset: Int, filter: MangaListFilter?): List<Manga> = runCatchingCompatibility {
		val uri = "content://${source.authority}/manga".toUri().buildUpon()
		uri.appendQueryParameter("offset", offset.toString())
		when (filter) {
			is MangaListFilter.Advanced -> {
				filter.tags.forEach { uri.appendQueryParameter("tag_include", it.key) }
				filter.tagsExclude.forEach { uri.appendQueryParameter("tag_exclude", it.key) }
				filter.states.forEach { uri.appendQueryParameter("state", it.name) }
				filter.locale?.let { uri.appendQueryParameter("locale", it.language) }
				filter.contentRating.forEach { uri.appendQueryParameter("content_rating", it.name) }
			}

			is MangaListFilter.Search -> {
				uri.appendQueryParameter("query", filter.query)
			}

			null -> Unit
		}
		contentResolver.query(uri.build(), null, null, null, filter?.sortOrder?.name)
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
	fun getDetails(manga: Manga) = runCatchingCompatibility {
		val chapters = queryChapters(manga.url)
		val details = queryDetails(manga.url)
		Manga(
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
	fun getPages(chapter: MangaChapter): List<MangaPage> = runCatchingCompatibility {
		val uri = "content://${source.authority}/chapters".toUri()
			.buildUpon()
			.appendPath(chapter.url)
			.build()
		contentResolver.query(uri, null, null, null, null)
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
	fun getTags(): Set<MangaTag> = runCatchingCompatibility {
		val uri = "content://${source.authority}/tags".toUri()
		contentResolver.query(uri, null, null, null, null)
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
						availableStates = cursor.getStringOrNull(COLUMN_STATES)
							?.split(',')
							?.mapNotNullTo(EnumSet.noneOf(MangaState::class.java)) {
								MangaState.entries.find(it)
							}.orEmpty(),
						availableContentRating = cursor.getStringOrNull(COLUMN_CONTENT_RATING)
							?.split(',')
							?.mapNotNullTo(EnumSet.noneOf(ContentRating::class.java)) {
								ContentRating.entries.find(it)
							}.orEmpty(),
						isMultipleTagsSupported = cursor.getBooleanOrDefault(COLUMN_MULTIPLE_TAGS_SUPPORTED, true),
						isTagsExclusionSupported = cursor.getBooleanOrDefault(COLUMN_TAGS_EXCLUSION_SUPPORTED, false),
						isSearchSupported = cursor.getBooleanOrDefault(COLUMN_SEARCH_SUPPORTED, true),
						contentType = cursor.getStringOrNull(COLUMN_CONTENT_TYPE)?.let {
							ContentType.entries.find(it)
						} ?: ContentType.OTHER,
						defaultSortOrder = cursor.getStringOrNull(COLUMN_DEFAULT_SORT_ORDER)?.let {
							SortOrder.entries.find(it)
						} ?: SortOrder.ALPHABETICAL,
						sourceLocale = cursor.getStringOrNull(COLUMN_LOCALE)?.toLocale() ?: Locale.ROOT,
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

	private fun SafeCursor.getManga() = Manga(
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

	private inline fun <R> runCatchingCompatibility(block: () -> R): R = try {
		block()
	} catch (e: NoSuchElementException) { // unknown column name
		throw IncompatiblePluginException(source.name, e)
	} catch (e: IllegalArgumentException) {
		throw IncompatiblePluginException(source.name, e)
	}

	private fun Cursor?.safe() = SafeCursor(this ?: throw IncompatiblePluginException(source.name, null))

	class MangaSourceCapabilities(
		val availableSortOrders: Set<SortOrder>,
		val availableStates: Set<MangaState>,
		val availableContentRating: Set<ContentRating>,
		val isMultipleTagsSupported: Boolean,
		val isTagsExclusionSupported: Boolean,
		val isSearchSupported: Boolean,
		val contentType: ContentType,
		val defaultSortOrder: SortOrder,
		val sourceLocale: Locale,
	)

	private companion object {

		const val COLUMN_SORT_ORDERS = "sort_orders"
		const val COLUMN_STATES = "states"
		const val COLUMN_CONTENT_RATING = "content_rating"
		const val COLUMN_MULTIPLE_TAGS_SUPPORTED = "multiple_tags_supported"
		const val COLUMN_TAGS_EXCLUSION_SUPPORTED = "tags_exclusion_supported"
		const val COLUMN_SEARCH_SUPPORTED = "search_supported"
		const val COLUMN_CONTENT_TYPE = "content_type"
		const val COLUMN_DEFAULT_SORT_ORDER = "default_sort_order"
		const val COLUMN_LOCALE = "locale"
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
	}
}
