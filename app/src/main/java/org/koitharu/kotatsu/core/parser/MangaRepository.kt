package org.koitharu.kotatsu.core.parser

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.*

interface MangaRepository {

	val source: MangaSource

	val sortOrders: Set<SortOrder>

	suspend fun getList(
		offset: Int,
		query: String? = null,
		tags: Set<MangaTag>? = null,
		sortOrder: SortOrder? = null,
	): List<Manga>

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageUrl(page: MangaPage): String

	suspend fun getTags(): Set<MangaTag>

	companion object : KoinComponent {

		operator fun invoke(source: MangaSource): MangaRepository {
			return if (source == MangaSource.LOCAL) {
				get<LocalMangaRepository>()
			} else {
				RemoteMangaRepository(source, get())
			}
		}
	}
}