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
package org.koitharu.kotatsu.core.local.cookies.cache

import okhttp3.Cookie

/**
 * A CookieCache handles the volatile cookie session storage.
 */
interface CookieCache : MutableIterable<Cookie> {
	/**
	 * Add all the new cookies to the session, existing cookies will be overwritten.
	 *
	 * @param newCookies
	 */
	fun addAll(newCookies: Collection<Cookie>)

	/**
	 * Clear all the cookies from the session.
	 */
	fun clear()
}