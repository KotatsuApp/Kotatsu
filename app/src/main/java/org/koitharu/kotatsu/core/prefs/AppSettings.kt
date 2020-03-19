package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.delegates.prefs.*

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

	val gridSize by IntPreferenceDelegate(
		resources.getString(R.string.key_grid_size),
		100
	)

	val readerPageSwitch by StringSetPreferenceDelegate(
		resources.getString(R.string.key_reader_switchers),
		setOf(PAGE_SWITCH_TAPS)
	)

	var isTrafficWarningEnabled by BoolPreferenceDelegate(
		resources.getString(R.string.key_traffic_warning),
		true
	)

	val appUpdateAuto by BoolPreferenceDelegate(
		resources.getString(R.string.key_app_update_auto),
		true
	)

	var appUpdate by LongPreferenceDelegate(
		resources.getString(R.string.key_app_update),
		0L
	)

	private var sourcesOrderStr by NullableStringPreferenceDelegate(resources.getString(R.string.key_sources_order))

	var sourcesOrder: List<Int>
		get() = sourcesOrderStr?.split('|')?.mapNotNull(String::toIntOrNull).orEmpty()
		set(value) {
			sourcesOrderStr = value.joinToString("|")
		}

	var hiddenSources by StringSetPreferenceDelegate(resources.getString(R.string.key_sources_hidden))

	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}

	companion object {

		const val PAGE_SWITCH_TAPS = "taps"
		const val PAGE_SWITCH_VOLUME_KEYS = "volume"
	}
}