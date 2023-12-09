package org.koitharu.kotatsu.core.cache

import androidx.collection.LruCache
import java.util.concurrent.TimeUnit

class ExpiringLruCache<T>(
	val maxSize: Int,
	private val lifetime: Long,
	private val timeUnit: TimeUnit,
) {

	private val cache = LruCache<ContentCache.Key, ExpiringValue<T>>(maxSize)

	operator fun get(key: ContentCache.Key): T? {
		val value = cache[key] ?: return null
		if (value.isExpired) {
			cache.remove(key)
		}
		return value.get()
	}

	operator fun set(key: ContentCache.Key, value: T) {
		cache.put(key, ExpiringValue(value, lifetime, timeUnit))
	}

	fun clear() {
		cache.evictAll()
	}

	fun trimToSize(size: Int) {
		cache.trimToSize(size)
	}
}
