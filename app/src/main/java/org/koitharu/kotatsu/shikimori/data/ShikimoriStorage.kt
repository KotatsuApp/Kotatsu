package org.koitharu.kotatsu.shikimori.data

import android.content.Context
import androidx.core.content.edit

private const val PREF_NAME = "shikimori"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"

class ShikimoriStorage(context: Context) {

	private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	var accessToken: String?
		get() = prefs.getString(KEY_ACCESS_TOKEN, null)
		set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

	var refreshToken: String?
		get() = prefs.getString(KEY_REFRESH_TOKEN, null)
		set(value) = prefs.edit { putString(KEY_REFRESH_TOKEN, value) }
}