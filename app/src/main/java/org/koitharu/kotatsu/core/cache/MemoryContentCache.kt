package org.koitharu.kotatsu.core.cache

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import kotlinx.coroutines.Deferred
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource

@Suppress("DeferredResultUnused")
class MemoryContentCache(application: Application) : ContentCache, ComponentCallbacks2 {

	init {
		application.registerComponentCallbacks(this)
	}

	private val detailsCache = DeferredLruCache<Manga>(4)
	private val pagesCache = DeferredLruCache<List<MangaPage>>(4)

	override val isCachingEnabled: Boolean = true

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

	override fun onConfigurationChanged(newConfig: Configuration) = Unit

	override fun onLowMemory() = Unit

	override fun onTrimMemory(level: Int) {
		trimCache(detailsCache, level)
		trimCache(pagesCache, level)
	}

	private fun trimCache(cache: DeferredLruCache<*>, level: Int) {
		when (level) {
			ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
			ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
			ComponentCallbacks2.TRIM_MEMORY_MODERATE -> cache.evictAll()

			ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
			ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
			ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.trimToSize(1)

			else -> cache.trimToSize(cache.maxSize() / 2)
		}
	}
}
