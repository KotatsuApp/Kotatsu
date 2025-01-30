package org.koitharu.kotatsu.settings.backup

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipOutput
import org.koitharu.kotatsu.core.backup.ExternalBackupStorage
import org.koitharu.kotatsu.core.backup.TelegramBackupUploader
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.CoroutineIntentService
import javax.inject.Inject

@AndroidEntryPoint
class PeriodicalBackupService : CoroutineIntentService() {

	@Inject
	lateinit var externalBackupStorage: ExternalBackupStorage

	@Inject
	lateinit var telegramBackupUploader: TelegramBackupUploader

	@Inject
	lateinit var repository: BackupRepository

	@Inject
	lateinit var settings: AppSettings

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		if (!settings.isPeriodicalBackupEnabled || settings.periodicalBackupDirectory == null) {
			return
		}
		val lastBackupDate = externalBackupStorage.getLastBackupDate()
		if (lastBackupDate != null && lastBackupDate.time + settings.periodicalBackupFrequencyMillis > System.currentTimeMillis()) {
			return
		}
		val output = BackupZipOutput.createTemp(applicationContext)
		try {
			output.use { backup ->
				backup.put(repository.createIndex())
				backup.put(repository.dumpHistory())
				backup.put(repository.dumpCategories())
				backup.put(repository.dumpFavourites())
				backup.put(repository.dumpBookmarks())
				backup.put(repository.dumpSources())
				backup.put(repository.dumpSettings())
				backup.finish()
			}
			externalBackupStorage.put(output.file)
			externalBackupStorage.trim(settings.periodicalBackupMaxCount)
			if (settings.isBackupTelegramUploadEnabled) {
				telegramBackupUploader.uploadBackup(output.file)
			}
		} finally {
			output.file.delete()
		}
	}

	override fun IntentJobContext.onError(error: Throwable) = Unit
}
