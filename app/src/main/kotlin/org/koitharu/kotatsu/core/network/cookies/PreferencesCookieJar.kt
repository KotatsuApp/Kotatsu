package org.koitharu.kotatsu.core.network.cookies

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.collection.ArrayMap
import androidx.core.content.edit
import androidx.core.util.Predicate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug

private const val PREFS_NAME = "cookies"

class PreferencesCookieJar(
	context: Context,
) : MutableCookieJar {

	private val cache = ArrayMap<String, CookieWrapper>()
	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
	private var isLoaded = false

	@WorkerThread
	@Synchronized
	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		loadPersistent()
		val expired = HashSet<String>()
		val result = ArrayList<Cookie>()
		for ((key, cookie) in cache) {
			if (cookie.isExpired()) {
				expired += key
			} else if (cookie.cookie.matches(url)) {
				result += cookie.cookie
			}
		}
		if (expired.isNotEmpty()) {
			cache.removeAll(expired)
			removePersistent(expired)
		}
		return result
	}

	@WorkerThread
	@Synchronized
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		val wrapped = cookies.map { CookieWrapper(it) }
		prefs.edit(commit = true) {
			for (cookie in wrapped) {
				val key = cookie.key()
				cache[key] = cookie
				if (cookie.cookie.persistent) {
					putString(key, cookie.encode())
				}
			}
		}
	}

	@Synchronized
	@WorkerThread
	override fun removeCookies(url: HttpUrl, predicate: Predicate<Cookie>?) {
		loadPersistent()
		val toRemove = HashSet<String>()
		for ((key, cookie) in cache) {
			if (cookie.isExpired() || cookie.cookie.matches(url)) {
				if (predicate == null || predicate.test(cookie.cookie)) {
					toRemove += key
				}
			}
		}
		if (toRemove.isNotEmpty()) {
			cache.removeAll(toRemove)
			removePersistent(toRemove)
		}
	}

	override suspend fun clear(): Boolean {
		cache.clear()
		withContext(Dispatchers.IO) {
			prefs.edit(commit = true) { clear() }
		}
		return true
	}

	@Synchronized
	private fun loadPersistent() {
		if (!isLoaded) {
			val map = prefs.all
			cache.ensureCapacity(map.size)
			for ((k, v) in map) {
				val cookie = try {
					CookieWrapper(v as String)
				} catch (e: Exception) {
					e.printStackTraceDebug()
					continue
				}
				cache[k] = cookie
			}
			isLoaded = true
		}
	}

	private fun removePersistent(keys: Collection<String>) {
		prefs.edit(commit = true) {
			for (key in keys) {
				remove(key)
			}
		}
	}
}
