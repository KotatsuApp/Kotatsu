package org.koitharu.kotatsu.settings.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.backup.BackupZipOutput.Companion.DIR_BACKUPS
import org.koitharu.kotatsu.core.backup.ExternalBackupStorage
import org.koitharu.kotatsu.core.backup.TelegramBackupUploader
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.resolveFile
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PeriodicalBackupSettingsViewModel @Inject constructor(
	private val settings: AppSettings,
	private val telegramUploader: TelegramBackupUploader,
	private val backupStorage: ExternalBackupStorage,
	@ApplicationContext private val appContext: Context,
) : BaseViewModel() {

	val lastBackupDate = MutableStateFlow<Date?>(null)
	val backupsDirectory = MutableStateFlow<String?>("")
	val isTelegramCheckLoading = MutableStateFlow(false)

	init {
		updateSummaryData()
	}

	fun checkTelegram() {
		launchJob(Dispatchers.Default) {
			try {
				isTelegramCheckLoading.value = true
				telegramUploader.sendTestMessage()
			} finally {
				isTelegramCheckLoading.value = false
			}
		}
	}

	fun updateSummaryData() {
		updateBackupsDirectory()
		updateLastBackupDate()
	}

	private fun updateBackupsDirectory() = launchJob(Dispatchers.Default) {
		val dir = settings.periodicalBackupDirectory
		backupsDirectory.value = if (dir != null) {
			dir.toUserFriendlyString()
		} else {
			(appContext.getExternalFilesDir(DIR_BACKUPS) ?: File(appContext.filesDir, DIR_BACKUPS)).path
		}
	}

	private fun updateLastBackupDate() = launchJob(Dispatchers.Default) {
		lastBackupDate.value = backupStorage.getLastBackupDate()
	}

	private fun Uri.toUserFriendlyString(): String? {
		val df = DocumentFile.fromTreeUri(appContext, this)
		if (df?.canWrite() != true) {
			return null
		}
		return resolveFile(appContext)?.path ?: toString()
	}
}
