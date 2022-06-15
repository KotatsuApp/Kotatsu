package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.*
import java.util.*

/**
 * This parser is just for parser development, it should not be used in releases
 */
class DummyParser(override val context: MangaLoaderContext) : MangaParser(MangaSource.DUMMY) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("", null)

	override val sortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override suspend fun getDetails(manga: Manga): Manga {
		TODO("Not yet implemented")
	}

	override suspend fun getList(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder,
	): List<Manga> {
		TODO("Not yet implemented")
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		TODO("Not yet implemented")
	}

	override suspend fun getTags(): Set<MangaTag> {
		TODO("Not yet implemented")
	}
}