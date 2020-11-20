package org.koitharu.kotatsu.settings

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.base.ui.dialog.StorageSelectDialog
import org.koitharu.kotatsu.base.ui.dialog.TextInputDialog
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.ext.getStorageName
import org.koitharu.kotatsu.utils.ext.md5
import org.koitharu.kotatsu.utils.ext.names
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import java.io.File


class MainSettingsFragment : BasePreferenceFragment(R.string.settings),
	SharedPreferences.OnSharedPreferenceChangeListener,
	StorageSelectDialog.OnStorageSelectListener {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_main)
		findPreference<SeekBarPreference>(AppSettings.KEY_GRID_SIZE)?.run {
			summary = "%d%%".format(value)
			setOnPreferenceChangeListener { preference, newValue ->
				preference.summary = "%d%%".format(newValue)
				true
			}
		}
		findPreference<MultiSelectListPreference>(AppSettings.KEY_READER_SWITCHERS)?.summaryProvider =
			MultiSummaryProvider(R.string.gestures_only)
		findPreference<MultiSelectListPreference>(AppSettings.KEY_TRACK_SOURCES)?.summaryProvider =
			MultiSummaryProvider(R.string.dont_check)
		findPreference<ListPreference>(AppSettings.KEY_ZOOM_MODE)?.run {
			entryValues = ZoomMode.values().names()
			setDefaultValue(ZoomMode.FIT_CENTER.name)
		}
		findPreference<ListPreference>(AppSettings.KEY_LIST_MODE)?.run {
			entryValues = ListMode.values().names()
			setDefaultValue(ListMode.GRID.name)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_APP_UPDATE_AUTO)?.run {
			isVisible = AppUpdateChecker.isUpdateSupported(context)
		}
		findPreference<Preference>(AppSettings.KEY_LOCAL_STORAGE)?.run {
			summary = settings.getStorageDir(context)?.getStorageName(context)
				?: getString(R.string.not_available)
		}
		findPreference<SwitchPreference>(AppSettings.KEY_PROTECT_APP)?.isChecked =
			!settings.appPassword.isNullOrEmpty()
		findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
			title = getString(R.string.app_version, BuildConfig.VERSION_NAME)
			isEnabled = AppUpdateChecker.isUpdateSupported(context)
		}
		settings.subscribe(this)
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
			AppSettings.KEY_THEME_AMOLED -> {
				findPreference<Preference>(key)?.setSummary(R.string.restart_required)
			}
			AppSettings.KEY_LOCAL_STORAGE -> {
				findPreference<Preference>(key)?.run {
					summary = settings.getStorageDir(context)?.getStorageName(context)
						?: getString(R.string.not_available)
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		findPreference<PreferenceScreen>(AppSettings.KEY_REMOTE_SOURCES)?.run {
			val total = MangaSource.values().size - 1
			summary = getString(
				R.string.enabled_d_from_d, total - settings.hiddenSources.size, total
			)
		}
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		return when (preference?.key) {
			AppSettings.KEY_NOTIFICATIONS_SETTINGS -> {
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
			AppSettings.KEY_LOCAL_STORAGE -> {
				val ctx = context ?: return false
				StorageSelectDialog.Builder(ctx, settings.getStorageDir(ctx), this)
					.setTitle(preference.title)
					.setNegativeButton(android.R.string.cancel)
					.create()
					.show()
				true
			}
			AppSettings.KEY_PROTECT_APP -> {
				if ((preference as? SwitchPreference ?: return false).isChecked) {
					enableAppProtection(preference)
				} else {
					settings.appPassword = null
				}
				true
			}
			AppSettings.KEY_APP_VERSION -> {
				checkForUpdates()
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onStorageSelected(file: File) {
		settings.setStorageDir(context ?: return, file)
	}

	private fun enableAppProtection(preference: SwitchPreference) {
		val ctx = preference.context ?: return
		val cancelListener =
			object : DialogInterface.OnCancelListener, DialogInterface.OnClickListener {

				override fun onCancel(dialog: DialogInterface?) {
					settings.appPassword = null
					preference.isChecked = false
					preference.isEnabled = true
				}

				override fun onClick(dialog: DialogInterface?, which: Int) = onCancel(dialog)
			}
		preference.isEnabled = false
		TextInputDialog.Builder(ctx)
			.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
			.setHint(R.string.enter_password)
			.setNegativeButton(android.R.string.cancel, cancelListener)
			.setOnCancelListener(cancelListener)
			.setPositiveButton(android.R.string.ok) { d, password ->
				if (password.isBlank()) {
					cancelListener.onCancel(d)
					return@setPositiveButton
				}
				TextInputDialog.Builder(ctx)
					.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
					.setHint(R.string.repeat_password)
					.setNegativeButton(android.R.string.cancel, cancelListener)
					.setOnCancelListener(cancelListener)
					.setPositiveButton(android.R.string.ok) { d2, password2 ->
						if (password == password2) {
							settings.appPassword = password.md5()
							preference.isChecked = true
							preference.isEnabled = true
						} else {
							cancelListener.onCancel(d2)
							Snackbar.make(
								listView,
								R.string.passwords_mismatch,
								Snackbar.LENGTH_SHORT
							).show()
						}
					}.setTitle(preference.title)
					.create()
					.show()
			}.setTitle(preference.title)
			.create()
			.show()
	}

	private fun checkForUpdates() {
		viewLifecycleScope.launch {
			findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
				setSummary(R.string.checking_for_updates)
				isSelectable = false
			}
			val result = AppUpdateChecker(activity ?: return@launch).checkNow()
			findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
				setSummary(
					when (result) {
						true -> R.string.check_for_updates
						false -> R.string.no_update_available
						null -> R.string.update_check_failed
					}
				)
				isSelectable = true
			}
		}
	}
}