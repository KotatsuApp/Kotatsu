/*
 * Copyright (C) 2016 Francisco José Montiel Navarro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koitharu.kotatsu.core.network.cookies

import okhttp3.Cookie
import okhttp3.HttpUrl
import org.koitharu.kotatsu.core.network.cookies.cache.CookieCache
import org.koitharu.kotatsu.core.network.cookies.persistence.CookiePersistor
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class PersistentCookieJar(
	private val cache: CookieCache,
	private val persistor: CookiePersistor
) : ClearableCookieJar {

	private var isLoaded = AtomicBoolean(false)

	@Synchronized
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		if (isLoaded.compareAndSet(false, true)) {
			cache.addAll(persistor.loadAll())
		}
		cache.addAll(cookies)
		persistor.saveAll(cookies.filter { it.persistent })
	}

	@Synchronized
	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		if (isLoaded.compareAndSet(false, true)) {
			cache.addAll(persistor.loadAll())
		}
		val cookiesToRemove: MutableList<Cookie> = ArrayList()
		val validCookies: MutableList<Cookie> = ArrayList()
		val it = cache.iterator()
		while (it.hasNext()) {
			val currentCookie = it.next()
			when {
				currentCookie.isExpired() -> {
					cookiesToRemove.add(currentCookie)
					it.remove()
				}
				currentCookie.matches(url) -> {
					validCookies.add(currentCookie)
				}
			}
		}
		persistor.removeAll(cookiesToRemove)
		return validCookies
	}

	@Synchronized
	override fun clearSession() {
		cache.clear()
		cache.addAll(persistor.loadAll())
	}

	@Synchronized
	override fun clear() {
		cache.clear()
		persistor.clear()
	}

	private companion object {

		fun Cookie.isExpired(): Boolean {
			return expiresAt < System.currentTimeMillis()
		}
	}
}