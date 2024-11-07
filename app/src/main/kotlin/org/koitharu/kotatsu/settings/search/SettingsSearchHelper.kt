package org.koitharu.kotatsu.settings.search

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.XmlRes
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.get
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.settings.AppearanceSettingsFragment
import org.koitharu.kotatsu.settings.DownloadsSettingsFragment
import org.koitharu.kotatsu.settings.NetworkSettingsFragment
import org.koitharu.kotatsu.settings.ReaderSettingsFragment
import org.koitharu.kotatsu.settings.ServicesSettingsFragment
import org.koitharu.kotatsu.settings.about.AboutSettingsFragment
import org.koitharu.kotatsu.settings.sources.SourcesSettingsFragment
import org.koitharu.kotatsu.settings.tracker.TrackerSettingsFragment
import org.koitharu.kotatsu.settings.userdata.UserDataSettingsFragment
import javax.inject.Inject

@Reusable
@SuppressLint("RestrictedApi")
class SettingsSearchHelper @Inject constructor(
	@ApplicationContext private val context: Context,
) {

	fun inflatePreferences(): List<SettingsItem> {
		val preferenceManager = PreferenceManager(context)
		val result = ArrayList<SettingsItem>()
		preferenceManager.inflateTo(result, R.xml.pref_appearance, emptyList(), AppearanceSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_sources, emptyList(), SourcesSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_reader, emptyList(), ReaderSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_network, emptyList(), NetworkSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_user_data, emptyList(), UserDataSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_downloads, emptyList(), DownloadsSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_tracker, emptyList(), TrackerSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_services, emptyList(), ServicesSettingsFragment::class.java)
		preferenceManager.inflateTo(result, R.xml.pref_about, emptyList(), AboutSettingsFragment::class.java)
		return result
	}

	private fun PreferenceManager.inflateTo(
		result: MutableList<SettingsItem>,
		@XmlRes resId: Int,
		breadcrumbs: List<String>,
		fragmentClass: Class<out PreferenceFragmentCompat>
	) {
		val screen = inflateFromResource(context, resId, null)
		val screenTitle = screen.title?.toString()
		screen.inflateTo(
			result = result,
			breadcrumbs = if (screenTitle.isNullOrEmpty()) breadcrumbs else breadcrumbs + screenTitle,
			fragmentClass = fragmentClass,
		)
	}

	private fun PreferenceScreen.inflateTo(
		result: MutableList<SettingsItem>,
		breadcrumbs: List<String>,
		fragmentClass: Class<out PreferenceFragmentCompat>
	): Unit = repeat(preferenceCount) { i ->
		val pref = this[i]
		if (pref is PreferenceScreen) {
			val screenTitle = pref.title?.toString()
			pref.inflateTo(
				result = result,
				breadcrumbs = if (screenTitle.isNullOrEmpty()) breadcrumbs else breadcrumbs + screenTitle,
				fragmentClass = fragmentClass,
			)
		} else {
			result.add(
				SettingsItem(
					key = pref.key ?: return@repeat,
					title = pref.title ?: return@repeat,
					breadcrumbs = breadcrumbs,
					fragmentClass = fragmentClass,
				),
			)
		}
	}
}
