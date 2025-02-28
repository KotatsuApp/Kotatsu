package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.AbstractMangaParser
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.model.search.MangaSearchQuery
import org.koitharu.kotatsu.parsers.model.search.MangaSearchQueryCapabilities
import java.util.EnumSet

/**
 * This parser is just for parser development, it should not be used in releases
 */
class DummyParser(context: MangaLoaderContext) : AbstractMangaParser(context, MangaParserSource.DUMMY) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("localhost")

	override val availableSortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override val searchQueryCapabilities: MangaSearchQueryCapabilities
		get() = MangaSearchQueryCapabilities()

	override suspend fun getDetails(manga: Manga): Manga = stub(manga)

	override suspend fun getFilterOptions(): MangaListFilterOptions = stub(null)

	override suspend fun getList(query: MangaSearchQuery): List<Manga> = stub(null)

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = stub(null)

	private fun stub(manga: Manga?): Nothing {
		throw UnsupportedSourceException("Usage of Dummy parser", manga)
	}
}
