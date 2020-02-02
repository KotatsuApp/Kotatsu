package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.delegates.prefs.EnumPreferenceDelegate

class AppSettings private constructor(resources: Resources, private val prefs: SharedPreferences) : SharedPreferences by prefs {

	constructor(context: Context) : this(context.resources, PreferenceManager.getDefaultSharedPreferences(context))

	var listMode by EnumPreferenceDelegate(ListMode::class.java, resources.getString(R.string.key_list_mode), ListMode.DETAILED_LIST)

	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}
}