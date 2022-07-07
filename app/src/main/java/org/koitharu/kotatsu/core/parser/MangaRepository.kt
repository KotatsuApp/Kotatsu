package org.koitharu.kotatsu.core.parser

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.core.network.MirrorsInterceptor
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.*
import java.lang.ref.WeakReference
import java.util.*

interface MangaRepository {

	val source: MangaSource

	val sortOrders: Set<SortOrder>

	suspend fun getList(offset: Int, query: String): List<Manga>

	suspend fun getList(offset: Int, tags: Set<MangaTag>?, sortOrder: SortOrder?): List<Manga>

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageUrl(page: MangaPage): String

	suspend fun getTags(): Set<MangaTag>

	companion object : KoinComponent {

		private val cache = EnumMap<MangaSource, WeakReference<RemoteMangaRepository>>(MangaSource::class.java)

		operator fun invoke(source: MangaSource): MangaRepository {
			if (source == MangaSource.LOCAL) {
				return get<LocalMangaRepository>()
			}
			cache[source]?.get()?.let { return it }
			return synchronized(cache) {
				cache[source]?.get()?.let { return it }
				val parser = MangaParser(source, get())
				get<MirrorsInterceptor>().register(parser)
				val repository = RemoteMangaRepository(parser)
				cache[source] = WeakReference(repository)
				repository
			}
		}
	}
}