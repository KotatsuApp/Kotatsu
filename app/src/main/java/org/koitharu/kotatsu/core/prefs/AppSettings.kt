package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.delegates.prefs.EnumPreferenceDelegate
import org.koitharu.kotatsu.utils.delegates.prefs.NullableStringPreferenceDelegate
import org.koitharu.kotatsu.utils.delegates.prefs.StringIntPreferenceDelegate

class AppSettings private constructor(resources: Resources, private val prefs: SharedPreferences) :
	SharedPreferences by prefs {

	constructor(context: Context) : this(
		context.resources,
		PreferenceManager.getDefaultSharedPreferences(context)
	)

	var listMode by EnumPreferenceDelegate(
		ListMode::class.java,
		resources.getString(R.string.key_list_mode),
		ListMode.DETAILED_LIST
	)

	val theme by StringIntPreferenceDelegate(
		resources.getString(R.string.key_theme),
		AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
	)

	private var sourcesOrderStr by NullableStringPreferenceDelegate(resources.getString(R.string.key_sources_order))

	var sourcesOrder: List<Int>
		get() = sourcesOrderStr?.split('|')?.mapNotNull(String::toIntOrNull).orEmpty()
		set(value) {
			sourcesOrderStr = value.joinToString("|")
		}

	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}
}