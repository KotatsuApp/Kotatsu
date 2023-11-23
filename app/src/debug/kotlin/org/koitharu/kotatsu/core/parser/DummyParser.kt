package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet

/**
 * This parser is just for parser development, it should not be used in releases
 */
class DummyParser(context: MangaLoaderContext) : MangaParser(context, MangaSource.DUMMY) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("")

	override val availableSortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override suspend fun getDetails(manga: Manga): Manga {
		TODO("Not yet implemented")
	}

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> {
		TODO("Not yet implemented")
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		TODO("Not yet implemented")
	}

	override suspend fun getAvailableTags(): Set<MangaTag> {
		TODO("Not yet implemented")
	}
}
