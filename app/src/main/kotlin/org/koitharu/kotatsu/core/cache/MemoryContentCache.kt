package org.koitharu.kotatsu.core.cache

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.concurrent.TimeUnit

class MemoryContentCache(application: Application) : ContentCache, ComponentCallbacks2 {

	init {
		application.registerComponentCallbacks(this)
	}

	private val detailsCache = ExpiringLruCache<SafeDeferred<Manga>>(4, 5, TimeUnit.MINUTES)
	private val pagesCache = ExpiringLruCache<SafeDeferred<List<MangaPage>>>(4, 10, TimeUnit.MINUTES)
	private val relatedMangaCache = ExpiringLruCache<SafeDeferred<List<Manga>>>(4, 10, TimeUnit.MINUTES)

	override val isCachingEnabled: Boolean = true

	override suspend fun getDetails(source: MangaSource, url: String): Manga? {
		return detailsCache[ContentCache.Key(source, url)]?.awaitOrNull()
	}

	override fun putDetails(source: MangaSource, url: String, details: SafeDeferred<Manga>) {
		detailsCache[ContentCache.Key(source, url)] = details
	}

	override suspend fun getPages(source: MangaSource, url: String): List<MangaPage>? {
		return pagesCache[ContentCache.Key(source, url)]?.awaitOrNull()
	}

	override fun putPages(source: MangaSource, url: String, pages: SafeDeferred<List<MangaPage>>) {
		pagesCache[ContentCache.Key(source, url)] = pages
	}

	override suspend fun getRelatedManga(source: MangaSource, url: String): List<Manga>? {
		return relatedMangaCache[ContentCache.Key(source, url)]?.awaitOrNull()
	}

	override fun putRelatedManga(source: MangaSource, url: String, related: SafeDeferred<List<Manga>>) {
		relatedMangaCache[ContentCache.Key(source, url)] = related
	}

	override fun clear(source: MangaSource) {
		clearCache(detailsCache, source)
		clearCache(pagesCache, source)
		clearCache(relatedMangaCache, source)
	}

	override fun onConfigurationChanged(newConfig: Configuration) = Unit

	override fun onLowMemory() = Unit

	override fun onTrimMemory(level: Int) {
		trimCache(detailsCache, level)
		trimCache(pagesCache, level)
		trimCache(relatedMangaCache, level)
	}

	private fun trimCache(cache: ExpiringLruCache<*>, level: Int) {
		when (level) {
			ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
			ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
			ComponentCallbacks2.TRIM_MEMORY_MODERATE -> cache.clear()

			ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
			ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
			ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.trimToSize(1)

			else -> cache.trimToSize(cache.maxSize / 2)
		}
	}

	private fun clearCache(cache: ExpiringLruCache<*>, source: MangaSource) {
		cache.forEach { key ->
			if (key.source == source) {
				cache.remove(key)
			}
		}
	}
}
