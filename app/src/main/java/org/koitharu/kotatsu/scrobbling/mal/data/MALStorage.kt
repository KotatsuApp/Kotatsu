package org.koitharu.kotatsu.scrobbling.mal.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import org.koitharu.kotatsu.scrobbling.mal.data.model.MALUser
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_NAME = "myanimelist"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER = "user"

@Singleton
class MALStorage @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	var accessToken: String?
		get() = prefs.getString(KEY_ACCESS_TOKEN, null)
		set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

	var refreshToken: String?
		get() = prefs.getString(KEY_REFRESH_TOKEN, null)
		set(value) = prefs.edit { putString(KEY_REFRESH_TOKEN, value) }

	var user: MALUser?
		get() = prefs.getString(KEY_USER, null)?.let {
			MALUser(JSONObject(it))
		}
		set(value) = prefs.edit {
			putString(KEY_USER, value?.toJson()?.toString())
		}

	fun clear() = prefs.edit {
		clear()
	}

}
