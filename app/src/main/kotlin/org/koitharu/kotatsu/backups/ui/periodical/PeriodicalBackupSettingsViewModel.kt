package org.koitharu.kotatsu.backups.ui.periodical

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.backups.domain.BackupUtils
import org.koitharu.kotatsu.backups.domain.ExternalBackupStorage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.resolveFile
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PeriodicalBackupSettingsViewModel @Inject constructor(
	private val settings: AppSettings,
	private val telegramUploader: TelegramBackupUploader,
	private val backupStorage: ExternalBackupStorage,
	@ApplicationContext private val appContext: Context,
) : BaseViewModel() {

	val isTelegramAvailable
		get() = telegramUploader.isAvailable

	val lastBackupDate = MutableStateFlow<Date?>(null)
	val backupsDirectory = MutableStateFlow<String?>("")
	val isTelegramCheckLoading = MutableStateFlow(false)
	val onActionDone = MutableEventFlow<ReversibleAction>()

	init {
		updateSummaryData()
	}

	fun checkTelegram() {
		launchJob(Dispatchers.Default) {
			try {
				isTelegramCheckLoading.value = true
				telegramUploader.sendTestMessage()
				onActionDone.call(ReversibleAction(R.string.connection_ok, null))
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
			BackupUtils.getAppBackupDir(appContext).path
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
