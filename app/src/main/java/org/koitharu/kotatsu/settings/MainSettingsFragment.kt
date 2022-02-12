package org.koitharu.kotatsu.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import leakcanary.LeakCanary
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.base.ui.dialog.StorageSelectDialog
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.settings.protect.ProtectSetupActivity
import org.koitharu.kotatsu.settings.utils.SliderPreference
import org.koitharu.kotatsu.utils.ext.getStorageName
import org.koitharu.kotatsu.utils.ext.names
import org.koitharu.kotatsu.utils.ext.setDefaultValueCompat
import java.io.File
import java.util.*


class MainSettingsFragment : BasePreferenceFragment(R.string.settings),
	SharedPreferences.OnSharedPreferenceChangeListener,
	StorageSelectDialog.OnStorageSelectListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_main)
		findPreference<SliderPreference>(AppSettings.KEY_GRID_SIZE)?.run {
			summary = "%d%%".format(value)
			setOnPreferenceChangeListener { preference, newValue ->
				preference.summary = "%d%%".format(newValue)
				true
			}
		}
		preferenceScreen?.findPreference<ListPreference>(AppSettings.KEY_LIST_MODE)?.run {
			entryValues = ListMode.values().names()
			setDefaultValueCompat(ListMode.GRID.name)
		}
		findPreference<SwitchPreference>(AppSettings.KEY_DYNAMIC_THEME)?.isVisible =
			AppSettings.isDynamicColorAvailable
		findPreference<ListPreference>(AppSettings.KEY_DATE_FORMAT)?.run {
			entryValues = arrayOf("", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd", "dd MMM yyyy", "MMM dd, yyyy")
			val now = Date().time
			entries = entryValues.map { value ->
				val formattedDate = settings.dateFormat(value.toString()).format(now)
				if (value == "") {
					"${context.getString(R.string.system_default)} ($formattedDate)"
				} else {
					"$value ($formattedDate)"
				}
			}.toTypedArray()
			setDefaultValueCompat("")
			summary = "%s"
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_LOCAL_STORAGE)?.run {
			summary = settings.getStorageDir(context)?.getStorageName(context)
				?: getString(R.string.not_available)
		}
		findPreference<SwitchPreference>(AppSettings.KEY_PROTECT_APP)?.isChecked =
			!settings.appPassword.isNullOrEmpty()
		settings.subscribe(this)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_settings, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_leaks -> {
				startActivity(LeakCanary.newLeakDisplayActivityIntent())
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		when (key) {
			AppSettings.KEY_THEME -> {
				AppCompatDelegate.setDefaultNightMode(settings.theme)
			}
			AppSettings.KEY_DYNAMIC_THEME -> {
				findPreference<Preference>(key)?.setSummary(R.string.restart_required)
			}
			AppSettings.KEY_THEME_AMOLED -> {
				findPreference<Preference>(key)?.setSummary(R.string.restart_required)
			}
			AppSettings.KEY_HIDE_TOOLBAR -> {
				findPreference<SwitchPreference>(key)?.setSummary(R.string.restart_required)
			}
			AppSettings.KEY_LOCAL_STORAGE -> {
				findPreference<Preference>(key)?.run {
					summary = settings.getStorageDir(context)?.getStorageName(context)
						?: getString(R.string.not_available)
				}
			}
			AppSettings.KEY_APP_PASSWORD -> {
				findPreference<SwitchPreference>(AppSettings.KEY_PROTECT_APP)
					?.isChecked = !settings.appPassword.isNullOrEmpty()
			}
		}
	}

	override fun onResume() {
		super.onResume()
		findPreference<PreferenceScreen>(AppSettings.KEY_REMOTE_SOURCES)?.run {
			val total = MangaSource.values().size - 1
			summary = getString(
				R.string.enabled_d_of_d, total - settings.hiddenSources.size, total
			)
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				val ctx = context ?: return false
				StorageSelectDialog.Builder(ctx, settings.getStorageDir(ctx), this)
					.setTitle(preference.title ?: "")
					.setNegativeButton(android.R.string.cancel)
					.create()
					.show()
				true
			}
			AppSettings.KEY_PROTECT_APP -> {
				val pref = (preference as? SwitchPreference ?: return false)
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

	override fun onStorageSelected(file: File) {
		settings.setStorageDir(context ?: return, file)
	}

}