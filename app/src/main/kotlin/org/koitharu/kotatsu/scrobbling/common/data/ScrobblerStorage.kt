package org.koitharu.kotatsu.scrobbling.common.data

import android.content.Context
import androidx.core.content.edit
import org.jsoup.internal.StringUtil.StringJoiner
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerUser

private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER = "user"

class ScrobblerStorage(context: Context, service: ScrobblerService) {

	private val prefs = context.getSharedPreferences(service.name, Context.MODE_PRIVATE)

	var accessToken: String?
		get() = prefs.getString(KEY_ACCESS_TOKEN, null)
		set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

	var refreshToken: String?
		get() = prefs.getString(KEY_REFRESH_TOKEN, null)
		set(value) = prefs.edit { putString(KEY_REFRESH_TOKEN, value) }

	var user: ScrobblerUser?
		get() = prefs.getString(KEY_USER, null)?.let {
			val lines = it.lines()
			if (lines.size != 4) {
				return@let null
			}
			ScrobblerUser(
				id = lines[0].toLong(),
				nickname = lines[1],
				avatar = lines[2].takeUnless(String::isEmpty),
				service = ScrobblerService.valueOf(lines[3]),
			)
		}
		set(value) = prefs.edit {
			if (value == null) {
				remove(KEY_USER)
				return@edit
			}
			val str = StringJoiner("\n")
				.add(value.id)
				.add(value.nickname)
				.add(value.avatar.orEmpty())
				.add(value.service.name)
				.complete()
			putString(KEY_USER, str)
		}

	operator fun get(key: String): String? = prefs.getString(key, null)

	operator fun set(key: String, value: String?) = prefs.edit { putString(key, value) }

	fun clear() = prefs.edit {
		clear()
	}
}
