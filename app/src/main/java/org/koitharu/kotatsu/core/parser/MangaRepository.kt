package org.koitharu.kotatsu.core.parser

import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.model.*

interface MangaRepository {

	val source: MangaSource

	val sortOrders: Set<SortOrder>

	suspend fun getList(offset: Int, query: String): List<Manga>

	suspend fun getList(offset: Int, tags: Set<MangaTag>?, sortOrder: SortOrder?): List<Manga>

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageUrl(page: MangaPage): String

	suspend fun getTags(): Set<MangaTag>

	@Singleton
	class Factory @Inject constructor(
		private val localMangaRepository: LocalMangaRepository,
		private val loaderContext: MangaLoaderContext,
	) {

		private val cache = EnumMap<MangaSource, WeakReference<RemoteMangaRepository>>(MangaSource::class.java)

		fun create(source: MangaSource): MangaRepository {
			if (source == MangaSource.LOCAL) {
				return localMangaRepository
			}
			cache[source]?.get()?.let { return it }
			return synchronized(cache) {
				cache[source]?.get()?.let { return it }
				val repository = RemoteMangaRepository(MangaParser(source, loaderContext))
				cache[source] = WeakReference(repository)
				repository
			}
		}
	}
}
