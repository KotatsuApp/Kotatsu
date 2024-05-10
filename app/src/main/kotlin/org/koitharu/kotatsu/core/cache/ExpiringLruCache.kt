package org.koitharu.kotatsu.core.cache

import androidx.collection.LruCache
import java.util.concurrent.TimeUnit
import org.koitharu.kotatsu.core.cache.MemoryContentCache.Key as CacheKey

class ExpiringLruCache<T>(
	val maxSize: Int,
	private val lifetime: Long,
	private val timeUnit: TimeUnit,
) : Iterable<CacheKey> {

	private val cache = LruCache<CacheKey, ExpiringValue<T>>(maxSize)

	override fun iterator(): Iterator<CacheKey> = cache.snapshot().keys.iterator()

	operator fun get(key: CacheKey): T? {
		val value = cache[key] ?: return null
		if (value.isExpired) {
			cache.remove(key)
		}
		return value.get()
	}

	operator fun set(key: CacheKey, value: T) {
		cache.put(key, ExpiringValue(value, lifetime, timeUnit))
	}

	fun clear() {
		cache.evictAll()
	}

	fun trimToSize(size: Int) {
		cache.trimToSize(size)
	}

	fun remove(key: CacheKey) {
		cache.remove(key)
	}
}
