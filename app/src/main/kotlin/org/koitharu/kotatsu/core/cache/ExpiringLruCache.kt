package org.koitharu.kotatsu.core.cache

import androidx.collection.SieveCache
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.concurrent.TimeUnit
import org.koitharu.kotatsu.core.cache.MemoryContentCache.Key as CacheKey

class ExpiringLruCache<T>(
	val maxSize: Int,
	private val lifetime: Long,
	private val timeUnit: TimeUnit,
) {

	private val cache = SieveCache<CacheKey, ExpiringValue<T>>(maxSize)

	@Synchronized
	operator fun get(key: CacheKey): T? {
		val value = cache[key] ?: return null
		if (value.isExpired) {
			cache.remove(key)
		}
		return value.get()
	}

	operator fun set(key: CacheKey, value: T) {
		val value = ExpiringValue(value, lifetime, timeUnit)
		synchronized(this) {
			cache.put(key, value)
		}
	}

	@Synchronized
	fun clear() {
		cache.evictAll()
	}

	@Synchronized
	fun trimToSize(size: Int) {
		cache.trimToSize(size)
	}

	@Synchronized
	fun remove(key: CacheKey) {
		cache.remove(key)
	}

	@Synchronized
	fun removeAll(source: MangaSource) {
		cache.removeIf { key, _ -> key.source == source }
	}
}
