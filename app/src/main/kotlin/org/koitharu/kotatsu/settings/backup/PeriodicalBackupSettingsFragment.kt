package org.koitharu.kotatsu.settings.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.backup.BackupZipOutput.Companion.DIR_BACKUPS
import org.koitharu.kotatsu.core.backup.ExternalBackupStorage
import org.koitharu.kotatsu.core.backup.TelegramBackupUploader
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.resolveFile
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PeriodicalBackupSettingsFragment : BasePreferenceFragment(R.string.periodic_backups),
	ActivityResultCallback<Uri?> {

	@Inject
	lateinit var backupStorage: ExternalBackupStorage

	@Inject
	lateinit var telegramBackupUploader: TelegramBackupUploader

	private val outputSelectCall = registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), this)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_backup_periodic)

		val openTelegramBotPreference = findPreference<Preference>("open_telegram_chat")

		openTelegramBotPreference?.setOnPreferenceClickListener {
			telegramBackupUploader.openTelegramBot(it.context, "kotatsu_backup_bot")
			true
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindOutputSummary()
		bindLastBackupInfo()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT -> outputSelectCall.tryLaunch(null)
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			context?.contentResolver?.takePersistableUriPermission(result, takeFlags)
			settings.periodicalBackupDirectory = result
			bindOutputSummary()
			bindLastBackupInfo()
		}
	}

	private fun bindOutputSummary() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_OUTPUT) ?: return
		viewLifecycleScope.launch {
			preference.summary = withContext(Dispatchers.Default) {
				val value = settings.periodicalBackupDirectory
				value?.toUserFriendlyString(preference.context) ?: preference.context.run {
					getExternalFilesDir(DIR_BACKUPS) ?: File(filesDir, DIR_BACKUPS)
				}.path
			}
		}
	}

	private fun bindLastBackupInfo() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_LAST) ?: return
		viewLifecycleScope.launch {
			val lastDate = withContext(Dispatchers.Default) {
				backupStorage.getLastBackupDate()
			}
			preference.summary = lastDate?.let {
				preference.context.getString(
					R.string.last_successful_backup,
					DateUtils.getRelativeTimeSpanString(it.time),
				)
			}
			preference.isVisible = lastDate != null
		}
	}

	private fun Uri.toUserFriendlyString(context: Context): String {
		val df = DocumentFile.fromTreeUri(context, this)
		if (df?.canWrite() != true) {
			return context.getString(R.string.invalid_value_message)
		}
		return resolveFile(context)?.path ?: toString()
	}
}

