package org.koitharu.kotatsu.settings.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import java.util.Date

@AndroidEntryPoint
class PeriodicalBackupSettingsFragment : BasePreferenceFragment(R.string.periodic_backups),
	ActivityResultCallback<Uri?> {

	private val viewModel by viewModels<PeriodicalBackupSettingsViewModel>()

	private val outputSelectCall = registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), this)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_backup_periodic)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.lastBackupDate.observe(viewLifecycleOwner, ::bindLastBackupInfo)
		viewModel.backupsDirectory.observe(viewLifecycleOwner, ::bindOutputSummary)
		viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(listView, this))
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		val result = when (preference.key) {
			AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT -> outputSelectCall.tryLaunch(null)
			else -> return super.onPreferenceTreeClick(preference)
		}
		if (!result) {
			Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
		}
		return true
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			context?.contentResolver?.takePersistableUriPermission(result, takeFlags)
			settings.periodicalBackupDirectory = result
			viewModel.updateSummaryData()
		}
	}

	private fun bindOutputSummary(path: String?) {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT) ?: return
		preference.summary = when (path) {
			null -> getString(R.string.invalid_value_message)
			"" -> null
			else -> path
		}
	}

	private fun bindLastBackupInfo(lastBackupDate: Date?) {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_LAST) ?: return
		preference.summary = lastBackupDate?.let {
			preference.context.getString(
				R.string.last_successful_backup,
				DateUtils.getRelativeTimeSpanString(it.time),
			)
		}
		preference.isVisible = lastBackupDate != null
	}
}

