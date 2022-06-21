package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*

class RemoteMangaRepository(private val parser: MangaParser) : MangaRepository {

	override val source: MangaSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.sortOrders

	var defaultSortOrder: SortOrder?
		get() = getConfig().defaultSortOrder ?: sortOrders.firstOrNull()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	override suspend fun getList(offset: Int, query: String): List<Manga> {
		return parser.getList(offset, query)
	}

	override suspend fun getList(offset: Int, tags: Set<MangaTag>?, sortOrder: SortOrder?): List<Manga> {
		return parser.getList(offset, tags, sortOrder)
	}

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