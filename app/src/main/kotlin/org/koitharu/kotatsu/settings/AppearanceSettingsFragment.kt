package org.koitharu.kotatsu.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.view.postDelayed
import androidx.preference.ListPreference
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.ui.util.ActivityRecreationHandle
import org.koitharu.kotatsu.core.util.ext.getLocalesConfig
import org.koitharu.kotatsu.core.util.ext.map
import org.koitharu.kotatsu.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.core.util.ext.toList
import org.koitharu.kotatsu.parsers.util.names
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.settings.utils.ActivityListPreference
import org.koitharu.kotatsu.settings.utils.SliderPreference
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AppearanceSettingsFragment :
	BasePreferenceFragment(R.string.appearance),
	SharedPreferences.OnSharedPreferenceChangeListener {

	@Inject
	lateinit var activityRecreationHandle: ActivityRecreationHandle

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_appearance)
		findPreference<SliderPreference>(AppSettings.KEY_GRID_SIZE)?.run {
			val pattern = context.getString(R.string.percent_string_pattern)
			summary = pattern.format(value.toString())
			setOnPreferenceChangeListener { preference, newValue ->
				preference.summary = pattern.format(newValue.toString())
				true
			}
		}
		findPreference<ListPreference>(AppSettings.KEY_LIST_MODE)?.run {
			entryValues = ListMode.values().names()
			setDefaultValueCompat(ListMode.GRID.name)
		}
		findPreference<ActivityListPreference>(AppSettings.KEY_APP_LOCALE)?.run {
			initLocalePicker(this)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				activityIntent = Intent(
					Settings.ACTION_APP_LOCALE_SETTINGS,
					Uri.fromParts("package", context.packageName, null),
				)
			}
			summaryProvider = Preference.SummaryProvider<ActivityListPreference> {
				val locale = AppCompatDelegate.getApplicationLocales().get(0)
				locale?.getDisplayName(locale)?.toTitleCase(locale) ?: getString(R.string.automatic)
			}
			setDefaultValueCompat("")
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_THEME -> {
				AppCompatDelegate.setDefaultNightMode(settings.theme)
			}

			AppSettings.KEY_COLOR_THEME,
			AppSettings.KEY_THEME_AMOLED -> {
				postRestart()
			}

			AppSettings.KEY_APP_LOCALE -> {
				AppCompatDelegate.setApplicationLocales(settings.appLocales)
			}
		}
	}

	private fun postRestart() {
		view?.postDelayed(400) {
			activityRecreationHandle.recreateAll()
		}
	}

	private fun initLocalePicker(preference: ListPreference) {
		val locales = resources.getLocalesConfig()
			.toList()
			.sortedWith(LocaleComparator(preference.context))
		preference.entries = Array(locales.size + 1) { i ->
			if (i == 0) {
				getString(R.string.automatic)
			} else {
				val lc = locales[i - 1]
				lc.getDisplayName(lc).toTitleCase(lc)
			}
		}
		preference.entryValues = Array(locales.size + 1) { i ->
			if (i == 0) {
				""
			} else {
				locales[i - 1].toLanguageTag()
			}
		}
	}

	private class LocaleComparator(context: Context) : Comparator<Locale> {

		private val deviceLocales = LocaleManagerCompat.getSystemLocales(context)
			.map { it.language }

		override fun compare(a: Locale, b: Locale): Int {
			return if (a === b) {
				0
			} else {
				val indexA = deviceLocales.indexOf(a.language)
				val indexB = deviceLocales.indexOf(b.language)
				if (indexA == -1 && indexB == -1) {
					compareValues(a.language, b.language)
				} else {
					-2 - (indexA - indexB)
				}
			}
		}
	}
}
