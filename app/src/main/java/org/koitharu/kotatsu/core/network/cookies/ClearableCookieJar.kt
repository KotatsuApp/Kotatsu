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

import okhttp3.CookieJar

/**
 * This interface extends [okhttp3.CookieJar] and adds methods to clear the cookies.
 */
interface ClearableCookieJar : CookieJar {

	/**
	 * Clear all the session cookies while maintaining the persisted ones.
	 */
	fun clearSession()

	/**
	 * Clear all the cookies from persistence and from the cache.
	 */
	fun clear()
}