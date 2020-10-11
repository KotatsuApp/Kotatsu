package org.koitharu.kotatsu.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.arrayMapOf
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.BasePreferenceFragment
import org.koitharu.kotatsu.ui.common.dialog.StorageSelectDialog
import org.koitharu.kotatsu.ui.list.ListModeSelectDialog
import org.koitharu.kotatsu.ui.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.ui.tracker.TrackWorker
import org.koitharu.kotatsu.utils.ext.getStorageName
import java.io.File


class MainSettingsFragment : BasePreferenceFragment(R.string.settings),
	SharedPreferences.OnSharedPreferenceChangeListener,
	StorageSelectDialog.OnStorageSelectListener {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_main)
		findPreference<Preference>(R.string.key_list_mode)?.summary =
			LIST_MODES[settings.listMode]?.let(::getString)
		findPreference<SeekBarPreference>(R.string.key_grid_size)?.run {
			summary = "%d%%".format(value)
			setOnPreferenceChangeListener { preference, newValue ->
				preference.summary = "%d%%".format(newValue)
				true
			}
		}
		findPreference<MultiSelectListPreference>(R.string.key_reader_switchers)?.summaryProvider =
			MultiSummaryProvider(R.string.gestures_only)
		findPreference<MultiSelectListPreference>(R.string.key_track_sources)?.summaryProvider =
			MultiSummaryProvider(R.string.dont_check)
		findPreference<Preference>(R.string.key_app_update_auto)?.run {
			isVisible = AppUpdateChecker.isUpdateSupported(context)
		}
		findPreference<Preference>(R.string.key_local_storage)?.run {
			summary = settings.getStorageDir(context)?.getStorageName(context)
				?: getString(R.string.not_available)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		when (key) {
			getString(R.string.key_list_mode) -> findPreference<Preference>(R.string.key_list_mode)?.summary =
				LIST_MODES[settings.listMode]?.let(::getString)
			getString(R.string.key_theme) -> {
				AppCompatDelegate.setDefaultNightMode(settings.theme)
			}
			getString(R.string.key_local_storage) -> {
				findPreference<Preference>(R.string.key_local_storage)?.run {
					summary = settings.getStorageDir(context)?.getStorageName(context)
						?: getString(R.string.not_available)
				}
			}
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

	override fun onResume() {
		super.onResume()
		findPreference<PreferenceScreen>(R.string.key_remote_sources)?.run {
			val total = MangaSource.values().size - 1
			summary = getString(
				R.string.enabled_d_from_d, total - settings.hiddenSources.size, total
			)
		}
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		return when (preference?.key) {
			getString(R.string.key_list_mode) -> {
				ListModeSelectDialog.show(childFragmentManager)
				true
			}
			getString(R.string.key_notifications_settings) -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
						.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
						.putExtra(Settings.EXTRA_CHANNEL_ID, TrackWorker.CHANNEL_ID)
					startActivity(intent)
				} else {
					(activity as? SettingsActivity)?.openNotificationSettingsLegacy()
				}
				true
			}
			getString(R.string.key_local_storage) -> {
				val ctx = context ?: return false
				StorageSelectDialog.Builder(ctx, settings.getStorageDir(ctx), this)
					.setTitle(preference.title)
					.setNegativeButton(android.R.string.cancel)
					.create()
					.show()
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onStorageSelected(file: File) {
		settings.setStorageDir(context ?: return, file)
	}

	private companion object {

		val LIST_MODES = arrayMapOf(
			ListMode.DETAILED_LIST to R.string.detailed_list,
			ListMode.GRID to R.string.grid,
			ListMode.LIST to R.string.list
		)
	}
}