package org.koitharu.kotatsu.settings.backup

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.prefs.AppSettings

class BackupSettingsFragment : BasePreferenceFragment(R.string.backup_restore),
	ActivityResultCallback<Uri> {

	private val backupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
		this
	)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_backup)
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_BACKUP -> {
				BackupDialogFragment().show(childFragmentManager, BackupDialogFragment.TAG)
				true
			}
			AppSettings.KEY_RESTORE -> {
				try {
					backupSelectCall.launch(arrayOf("*/*"))
				} catch (e: ActivityNotFoundException) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace()
					}
					Snackbar.make(
						listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT
					).show()
				}
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onActivityResult(result: Uri?) {
		RestoreDialogFragment.newInstance(result ?: return)
			.show(childFragmentManager, BackupDialogFragment.TAG)
	}
}