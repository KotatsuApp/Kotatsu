package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.newParser

class RemoteMangaRepository(
	override val source: MangaSource,
	loaderContext: MangaLoaderContext,
) : MangaRepository {

	private val parser: MangaParser = source.newParser(loaderContext)

	override val sortOrders: Set<SortOrder>
		get() = parser.sortOrders

	var defaultSortOrder: SortOrder?
		get() = getConfig().defaultSortOrder ?: sortOrders.firstOrNull()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	override suspend fun getList(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder?
	): List<Manga> = parser.getList(offset, query, tags, sortOrder)

	override suspend fun getDetails(manga: Manga): Manga = parser.getDetails(manga)

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = parser.getPages(chapter)

	override suspend fun getPageUrl(page: MangaPage): String = parser.getPageUrl(page)

	override suspend fun getTags(): Set<MangaTag> = parser.getTags()

	fun getFaviconUrl(): String = parser.getFaviconUrl()

	fun getAuthProvider(): MangaParserAuthProvider? = parser as? MangaParserAuthProvider

	fun getConfigKeys(): List<ConfigKey<*>> = ArrayList<ConfigKey<*>>().also {
		parser.onCreateConfig(it)
	}

	private fun getConfig() = parser.config as SourceSettings
}
