package org.koitharu.kotatsu.core.parser

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet

class DummyParser(context: MangaLoaderContext) : MangaParser(context, MangaSource.DUMMY) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("localhost")

	override val sortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override suspend fun getDetails(manga: Manga): Manga = stub()

	override suspend fun getList(
		offset: Int,
		query: String?,
		tags: Set<MangaTag>?,
		sortOrder: SortOrder,
	): List<Manga> = stub()

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = stub()

	override suspend fun getTags(): Set<MangaTag> = stub()

	private fun stub(): Nothing {
		throw NotFoundException("Usage of Dummy parser in release build", "")
	}
}
