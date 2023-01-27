package org.koitharu.kotatsu.core.cache

import androidx.collection.LruCache

class DeferredLruCache<T>(maxSize: Int) : LruCache<ContentCache.Key, SafeDeferred<T>>(maxSize)
