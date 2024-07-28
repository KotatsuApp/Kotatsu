package org.koitharu.kotatsu.core.parser.external

import android.content.ContentResolver
import android.database.Cursor
import androidx.collection.ArraySet
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
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

class ExternalMangaRepository(
	private val contentResolver: ContentResolver,
	override val source: ExternalMangaSource,
	cache: MemoryContentCache,
) : CachingMangaRepository(cache) {

	private val capabilities by lazy { queryCapabilities() }

	override val sortOrders: Set<SortOrder>
		get() = capabilities?.availableSortOrders ?: EnumSet.of(SortOrder.ALPHABETICAL)
	override val states: Set<MangaState>
		get() = capabilities?.availableStates.orEmpty()
	override val contentRatings: Set<ContentRating>
		get() = capabilities?.availableContentRating.orEmpty()
	override var defaultSortOrder: SortOrder
		get() = capabilities?.defaultSortOrder ?: SortOrder.ALPHABETICAL
		set(value) = Unit
	override val isMultipleTagsSupported: Boolean
		get() = capabilities?.isMultipleTagsSupported ?: true
	override val isTagsExclusionSupported: Boolean
		get() = capabilities?.isTagsExclusionSupported ?: false
	override val isSearchSupported: Boolean
		get() = capabilities?.isSearchSupported ?: true

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> =
		runInterruptible(Dispatchers.Default) {
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
			contentResolver.query(uri.build(), null, null, null, filter?.sortOrder?.name)?.use { cursor ->
				val result = ArrayList<Manga>(cursor.count)
				if (cursor.moveToFirst()) {
					do {
						result += cursor.getManga()
					} while (cursor.moveToNext())
				}
				result
			}.orEmpty()
		}

	override suspend fun getDetailsImpl(manga: Manga): Manga = coroutineScope {
		val chapters = async { queryChapters(manga.url) }
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
			chapters = chapters.await(),
			source = source,
		)
	}

	override suspend fun getPagesImpl(chapter: MangaChapter): List<MangaPage> = runInterruptible(Dispatchers.Default) {
		val uri = "content://${source.authority}/chapters".toUri()
			.buildUpon()
			.appendPath(chapter.url)
			.build()
		contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			val result = ArrayList<MangaPage>(cursor.count)
			if (cursor.moveToFirst()) {
				do {
					result += MangaPage(
						id = cursor.getLong(0),
						url = cursor.getString(1),
						preview = cursor.getStringOrNull(2),
						source = source,
					)
				} while (cursor.moveToNext())
			}
			result
		}.orEmpty()
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url

	override suspend fun getTags(): Set<MangaTag> = runInterruptible(Dispatchers.Default) {
		val uri = "content://${source.authority}/tags".toUri()
		contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			val result = ArraySet<MangaTag>(cursor.count)
			if (cursor.moveToFirst()) {
				do {
					result += MangaTag(
						key = cursor.getString(0),
						title = cursor.getString(1),
						source = source,
					)
				} while (cursor.moveToNext())
			}
			result
		}.orEmpty()
	}

	override suspend fun getLocales(): Set<Locale> = emptySet()

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = emptyList() // TODO

	private suspend fun queryDetails(url: String): Manga = runInterruptible(Dispatchers.Default) {
		val uri = "content://${source.authority}/manga".toUri()
			.buildUpon()
			.appendPath(url)
			.build()
		checkNotNull(
			contentResolver.query(uri, null, null, null, null)?.use { cursor ->
				cursor.moveToFirst()
				cursor.getManga()
			},
		)
	}

	private suspend fun queryChapters(url: String): List<MangaChapter>? = runInterruptible(Dispatchers.Default) {
		val uri = "content://${source.authority}/manga/chapters".toUri()
			.buildUpon()
			.appendPath(url)
			.build()
		contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			val result = ArrayList<MangaChapter>(cursor.count)
			if (cursor.moveToFirst()) {
				do {
					result += MangaChapter(
						id = cursor.getLong(0),
						name = cursor.getString(1),
						number = cursor.getFloat(2),
						volume = cursor.getInt(3),
						url = cursor.getString(4),
						scanlator = cursor.getStringOrNull(5),
						uploadDate = cursor.getLong(6),
						branch = cursor.getStringOrNull(7),
						source = source,
					)
				} while (cursor.moveToNext())
			}
			result
		}
	}

	private fun Cursor.getManga() = Manga(
		id = getLong(0),
		title = getString(1),
		altTitle = getStringOrNull(2),
		url = getString(3),
		publicUrl = getString(4),
		rating = getFloat(5),
		isNsfw = getInt(6) > 1,
		coverUrl = getString(7),
		tags = getStringOrNull(8)?.split(':')?.mapNotNullToSet {
			val parts = it.splitTwoParts('=') ?: return@mapNotNullToSet null
			MangaTag(key = parts.first, title = parts.second, source = source)
		}.orEmpty(),
		state = getStringOrNull(9)?.let { MangaState.entries.find(it) },
		author = optString(10),
		largeCoverUrl = optString(11),
		description = optString(12),
		chapters = emptyList(),
		source = source,
	)

	private fun Cursor.optString(columnIndex: Int): String? {
		return if (isNull(columnIndex)) {
			null
		} else {
			getString(columnIndex)
		}
	}

	private fun queryCapabilities(): MangaSourceCapabilities? {
		val uri = "content://${source.authority}/capabilities".toUri()
		return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			if (cursor.moveToFirst()) {
				MangaSourceCapabilities(
					availableSortOrders = cursor.getStringOrNull(0)
						?.split(',')
						?.mapNotNullTo(EnumSet.noneOf(SortOrder::class.java)) {
							SortOrder.entries.find(it)
						}.orEmpty(),
					availableStates = cursor.getStringOrNull(1)
						?.split(',')
						?.mapNotNullTo(EnumSet.noneOf(MangaState::class.java)) {
							MangaState.entries.find(it)
						}.orEmpty(),
					availableContentRating = cursor.getStringOrNull(2)
						?.split(',')
						?.mapNotNullTo(EnumSet.noneOf(ContentRating::class.java)) {
							ContentRating.entries.find(it)
						}.orEmpty(),
					isMultipleTagsSupported = cursor.getInt(3) > 1,
					isTagsExclusionSupported = cursor.getInt(4) > 1,
					isSearchSupported = cursor.getInt(5) > 1,
					contentType = ContentType.entries.find(cursor.getString(6)) ?: ContentType.OTHER,
					defaultSortOrder = cursor.getStringOrNull(7)?.let {
						SortOrder.entries.find(it)
					} ?: SortOrder.ALPHABETICAL,
					sourceLocale = cursor.getStringOrNull(8)?.let { Locale(it) } ?: Locale.ROOT,
				)
			} else {
				null
			}
		}
	}

	private class MangaSourceCapabilities(
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
}
