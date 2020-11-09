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
package org.koitharu.kotatsu.core.network.cookies.persistence

import android.content.Context
import okhttp3.Cookie
import java.util.*

class SharedPrefsCookiePersistor(context: Context) : CookiePersistor {

	private val sharedPreferences by lazy {
		context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
	}

	override fun loadAll(): List<Cookie> {
		val cookies: MutableList<Cookie> = ArrayList(sharedPreferences.all.size)
		for ((_, value) in sharedPreferences.all) {
			val serializedCookie = value as? String
			if (serializedCookie != null) {
				val cookie = SerializableCookie().decode(serializedCookie)
				if (cookie != null) {
					cookies.add(cookie)
				}
			}
		}
		return cookies
	}

	override fun saveAll(cookies: Collection<Cookie>) {
		val editor = sharedPreferences.edit()
		for (cookie in cookies) {
			editor.putString(createCookieKey(cookie), SerializableCookie().encode(cookie))
		}
		editor.apply()
	}

	override fun removeAll(cookies: Collection<Cookie>) {
		val editor = sharedPreferences.edit()
		for (cookie in cookies) {
			editor.remove(createCookieKey(cookie))
		}
		editor.apply()
	}

	override fun clear() {
		sharedPreferences.edit().clear().apply()
	}

	private companion object {

		fun createCookieKey(cookie: Cookie): String {
			return (if (cookie.secure) "https" else "http") + "://" + cookie.domain + cookie.path + "|" + cookie.name
		}
	}

}