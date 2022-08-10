package org.koitharu.kotatsu.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.postDelayed
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.base.ui.util.ActivityRecreationHandle
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.parsers.util.names
import org.koitharu.kotatsu.settings.protect.ProtectSetupActivity
import org.koitharu.kotatsu.settings.utils.SliderPreference
import org.koitharu.kotatsu.utils.ext.setDefaultValueCompat

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
		preferenceScreen?.findPreference<ListPreference>(AppSettings.KEY_LIST_MODE)?.run {
			entryValues = ListMode.values().names()
			setDefaultValueCompat(ListMode.GRID.name)
		}
		findPreference<Preference>(AppSettings.KEY_DYNAMIC_THEME)?.isVisible = DynamicColors.isDynamicColorAvailable()
		findPreference<ListPreference>(AppSettings.KEY_DATE_FORMAT)?.run {
			entryValues = resources.getStringArray(R.array.date_formats)
			val now = Date().time
			entries = entryValues.map { value ->
				val formattedDate = settings.getDateFormat(value.toString()).format(now)
				if (value == "") {
					"${context.getString(R.string.system_default)} ($formattedDate)"
				} else {
					formattedDate
				}
			}.toTypedArray()
			setDefaultValueCompat("")
			summary = "%s"
		}
		findPreference<TwoStatePreference>(AppSettings.KEY_PROTECT_APP)
			?.isChecked = !settings.appPassword.isNullOrEmpty()
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
			AppSettings.KEY_DYNAMIC_THEME -> {
				postRestart()
			}
			AppSettings.KEY_THEME_AMOLED -> {
				postRestart()
			}
			AppSettings.KEY_APP_PASSWORD -> {
				findPreference<TwoStatePreference>(AppSettings.KEY_PROTECT_APP)
					?.isChecked = !settings.appPassword.isNullOrEmpty()
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_PROTECT_APP -> {
				val pref = (preference as? TwoStatePreference ?: return false)
				if (pref.isChecked) {
					pref.isChecked = false
					startActivity(Intent(preference.context, ProtectSetupActivity::class.java))
				} else {
					settings.appPassword = null
				}
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun postRestart() {
		view?.postDelayed(400) {
			activityRecreationHandle.recreateAll()
		}
	}
}
