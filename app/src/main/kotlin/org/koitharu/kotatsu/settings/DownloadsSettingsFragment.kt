package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.ui.dialog.StorageSelectDialog
import org.koitharu.kotatsu.core.util.ext.getStorageName
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsSettingsFragment :
	BasePreferenceFragment(R.string.downloads),
	SharedPreferences.OnSharedPreferenceChangeListener,
	StorageSelectDialog.OnStorageSelectListener {

	@Inject
	lateinit var storageManager: LocalStorageManager

	@Inject
	lateinit var downloadsScheduler: DownloadWorker.Scheduler

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_downloads)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_LOCAL_STORAGE)?.bindStorageName()
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				findPreference<Preference>(key)?.bindStorageName()
			}

			AppSettings.KEY_DOWNLOADS_WIFI -> {
				updateDownloadsConstraints()
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				val ctx = context ?: return false
				StorageSelectDialog.Builder(ctx, storageManager, this)
					.setTitle(preference.title ?: "")
					.setNegativeButton(android.R.string.cancel)
					.create()
					.show()
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onStorageSelected(file: File) {
		settings.mangaStorageDir = file
	}

	private fun Preference.bindStorageName() {
		viewLifecycleScope.launch {
			val storage = storageManager.getDefaultWriteableDir()
			summary = storage?.getStorageName(context) ?: getString(R.string.not_available)
		}
	}

	private fun updateDownloadsConstraints() {
		val preference = findPreference<Preference>(AppSettings.KEY_DOWNLOADS_WIFI)
		viewLifecycleScope.launch {
			try {
				preference?.isEnabled = false
				withContext(Dispatchers.Default) {
					downloadsScheduler.updateConstraints()
				}
			} catch (e: Exception) {
				e.printStackTraceDebug()
			} finally {
				preference?.isEnabled = true
			}
		}
	}
}
