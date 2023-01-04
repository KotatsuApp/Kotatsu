package org.koitharu.kotatsu.core.cache

import androidx.collection.LruCache
import kotlinx.coroutines.Deferred

class DeferredLruCache<T>(maxSize: Int) : LruCache<ContentCache.Key, Deferred<T>>(maxSize) {

	override fun entryRemoved(
		evicted: Boolean,
		key: ContentCache.Key,
		oldValue: Deferred<T>,
		newValue: Deferred<T>?,
	) {
		super.entryRemoved(evicted, key, oldValue, newValue)
		oldValue.cancel()
	}
}
