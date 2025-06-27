package org.koitharu.kotatsu.backups.ui.backup

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.backups.data.BackupRepository
import org.koitharu.kotatsu.backups.ui.BaseBackupRestoreService
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.util.CompositeResult
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.powerManager
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.core.util.ext.withPartialWakeLock
import org.koitharu.kotatsu.core.util.progress.Progress
import java.io.FileNotFoundException
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import androidx.appcompat.R as appcompatR

@AndroidEntryPoint
@SuppressLint("InlinedApi")
class BackupService : BaseBackupRestoreService() {

	override val notificationTag = TAG
	override val isRestoreService = false

	@Inject
	lateinit var repository: BackupRepository

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		val notification = buildNotification(Progress.INDETERMINATE)
		setForeground(
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
		val destination = intent.getStringExtra(AppRouter.KEY_DATA)?.toUriOrNull() ?: throw FileNotFoundException()
		powerManager.withPartialWakeLock(TAG) {
			val progress = MutableStateFlow(Progress.INDETERMINATE)
			val progressUpdateJob = if (checkNotificationPermission(CHANNEL_ID)) {
				launch {
					progress.collect {
						notificationManager.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(it))
					}
				}
			} else {
				null
			}
			try {
				ZipOutputStream(contentResolver.openOutputStream(destination)).use { output ->
					repository.createBackup(output, progress)
				}
			} catch (e: Throwable) {
				try {
					DocumentFile.fromSingleUri(applicationContext, destination)?.delete()
				} catch (e2: Throwable) {
					e.addSuppressed(e2)
				}
				throw e
			}
			progressUpdateJob?.cancelAndJoin()
			contentResolver.notifyChange(destination, null)
			showResultNotification(destination, CompositeResult.success())
			withContext(Dispatchers.Main) {
				Toast.makeText(this@BackupService, R.string.backup_saved, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun IntentJobContext.buildNotification(progress: Progress): Notification {
		return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(getString(R.string.creating_backup))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setDefaults(0)
			.setSilent(true)
			.setOngoing(true)
			.setProgress(
				progress.total.coerceAtLeast(0),
				progress.progress.coerceAtLeast(0),
				progress.isIndeterminate,
			)
			.setContentText(
				if (progress.isIndeterminate) {
					getString(R.string.processing_)
				} else {
					getString(R.string.fraction_pattern, progress.progress, progress.total)
				},
			)
			.setSmallIcon(android.R.drawable.stat_sys_upload)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.addAction(
				appcompatR.drawable.abc_ic_clear_material,
				applicationContext.getString(android.R.string.cancel),
				getCancelIntent(),
			).build()
	}

	companion object {

		private const val TAG = "BACKUP"
		private const val FOREGROUND_NOTIFICATION_ID = 33

		@CheckResult
		fun start(context: Context, uri: Uri): Boolean = try {
			val intent = Intent(context, BackupService::class.java)
			intent.putExtra(AppRouter.KEY_DATA, uri.toString())
			ContextCompat.startForegroundService(context, intent)
			true
		} catch (e: Exception) {
			e.printStackTraceDebug()
			false
		}
	}
}
