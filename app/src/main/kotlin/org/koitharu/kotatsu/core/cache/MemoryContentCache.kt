package org.koitharu.kotatsu.core.cache

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryContentCache @Inject constructor(application: Application) : ComponentCallbacks2 {

	private val isLowRam = application.isLowRamDevice()

	private val detailsCache = ExpiringLruCache<SafeDeferred<Manga>>(if (isLowRam) 1 else 4, 5, TimeUnit.MINUTES)
	private val pagesCache =
		ExpiringLruCache<SafeDeferred<List<MangaPage>>>(if (isLowRam) 1 else 4, 10, TimeUnit.MINUTES)
	private val relatedMangaCache =
		ExpiringLruCache<SafeDeferred<List<Manga>>>(if (isLowRam) 1 else 3, 10, TimeUnit.MINUTES)

	init {
		application.registerComponentCallbacks(this)
	}

	suspend fun getDetails(source: MangaSource, url: String): Manga? {
		return detailsCache[Key(source, url)]?.awaitOrNull()
	}

	fun putDetails(source: MangaSource, url: String, details: SafeDeferred<Manga>) {
		detailsCache[Key(source, url)] = details
	}

	suspend fun getPages(source: MangaSource, url: String): List<MangaPage>? {
		return pagesCache[Key(source, url)]?.awaitOrNull()
	}

	fun putPages(source: MangaSource, url: String, pages: SafeDeferred<List<MangaPage>>) {
		pagesCache[Key(source, url)] = pages
	}

	suspend fun getRelatedManga(source: MangaSource, url: String): List<Manga>? {
		return relatedMangaCache[Key(source, url)]?.awaitOrNull()
	}

	fun putRelatedManga(source: MangaSource, url: String, related: SafeDeferred<List<Manga>>) {
		relatedMangaCache[Key(source, url)] = related
	}

	fun clear(source: MangaSource) {
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

	data class Key(
		val source: MangaSource,
		val url: String,
	)
}
