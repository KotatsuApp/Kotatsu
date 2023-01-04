package org.koitharu.kotatsu.core.cache

import androidx.collection.LruCache

class DeferredLruCache<T>(maxSize: Int) : LruCache<ContentCache.Key, SafeDeferred<T>>(maxSize) {

	override fun entryRemoved(
		evicted: Boolean,
		key: ContentCache.Key,
		oldValue: SafeDeferred<T>,
		newValue: SafeDeferred<T>?,
	) {
		super.entryRemoved(evicted, key, oldValue, newValue)
		oldValue.cancel()
	}
}
