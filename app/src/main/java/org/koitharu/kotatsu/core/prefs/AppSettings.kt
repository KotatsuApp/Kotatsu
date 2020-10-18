package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.arraySetOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.LocalMangaRepository
import org.koitharu.kotatsu.utils.delegates.prefs.*
import java.io.File

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

	var defaultSection by EnumPreferenceDelegate(
		AppSection::class.java,
		resources.getString(R.string.key_app_section),
		AppSection.HISTORY
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
		arraySetOf(PAGE_SWITCH_TAPS)
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

	val trackerNotifications by BoolPreferenceDelegate(
		resources.getString(R.string.key_tracker_notifications),
		true
	)

	var notificationSound by StringPreferenceDelegate(
		resources.getString(R.string.key_notifications_sound),
		Settings.System.DEFAULT_NOTIFICATION_URI.toString()
	)

	val notificationVibrate by BoolPreferenceDelegate(
		resources.getString(R.string.key_notifications_vibrate),
		false
	)

	val notificationLight by BoolPreferenceDelegate(
		resources.getString(R.string.key_notifications_light),
		true
	)

	val readerAnimation by BoolPreferenceDelegate(
		resources.getString(R.string.key_reader_animation),
		false
	)

	val trackSources by StringSetPreferenceDelegate(
		resources.getString(R.string.key_track_sources),
		arraySetOf(TRACK_FAVOURITES, TRACK_HISTORY)
	)

	var appPassword by NullableStringPreferenceDelegate(
		resources.getString(R.string.key_app_password)
	)

	private var sourcesOrderStr by NullableStringPreferenceDelegate(
		resources.getString(R.string.key_sources_order)
	)

	var sourcesOrder: List<Int>
		get() = sourcesOrderStr?.split('|')?.mapNotNull(String::toIntOrNull).orEmpty()
		set(value) {
			sourcesOrderStr = value.joinToString("|")
		}

	var hiddenSources by StringSetPreferenceDelegate(resources.getString(R.string.key_sources_hidden))

	fun getStorageDir(context: Context): File? {
		val value = prefs.getString(context.getString(R.string.key_local_storage), null)?.let {
			File(it)
		}?.takeIf { it.exists() && it.canWrite() }
		return value ?: LocalMangaRepository.getFallbackStorageDir(context)
	}

	fun setStorageDir(context: Context, file: File?) {
		val key = context.getString(R.string.key_local_storage)
		prefs.edit {
			if (file == null) {
				remove(key)
			} else {
				putString(key, file.path)
			}
		}
	}

	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}

	companion object {

		const val PAGE_SWITCH_TAPS = "taps"
		const val PAGE_SWITCH_VOLUME_KEYS = "volume"

		const val TRACK_HISTORY = "history"
		const val TRACK_FAVOURITES = "favourites"
	}
}