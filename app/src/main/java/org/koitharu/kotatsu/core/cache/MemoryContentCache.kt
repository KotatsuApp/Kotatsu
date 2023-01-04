package org.koitharu.kotatsu.core.cache

import kotlinx.coroutines.Deferred
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource

@Suppress("DeferredResultUnused")
class MemoryContentCache : ContentCache {

	private val detailsCache = DeferredLruCache<Manga>(10)
	private val pagesCache = DeferredLruCache<List<MangaPage>>(10)

	override suspend fun getDetails(source: MangaSource, url: String): Manga? {
		return detailsCache[ContentCache.Key(source, url)]?.await()
	}

	override fun putDetails(source: MangaSource, url: String, details: Deferred<Manga>) {
		detailsCache.put(ContentCache.Key(source, url), details)
	}

	override suspend fun getPages(source: MangaSource, url: String): List<MangaPage>? {
		return pagesCache[ContentCache.Key(source, url)]?.await()
	}

	override fun putPages(source: MangaSource, url: String, pages: Deferred<List<MangaPage>>) {
		pagesCache.put(ContentCache.Key(source, url), pages)
	}
}
