package org.koitharu.kotatsu.backups.ui.restore

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.backups.data.BackupRepository
import org.koitharu.kotatsu.backups.domain.BackupSection
import org.koitharu.kotatsu.backups.ui.BaseBackupRestoreService
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.powerManager
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.core.util.ext.withPartialWakeLock
import org.koitharu.kotatsu.core.util.progress.Progress
import java.io.FileNotFoundException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import androidx.appcompat.R as appcompatR

@AndroidEntryPoint
@SuppressLint("InlinedApi")
class RestoreService : BaseBackupRestoreService() {

	override val notificationTag = TAG

	@Inject
	lateinit var repository: BackupRepository

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		val notification = buildNotification(Progress.INDETERMINATE)
		setForeground(
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
		val source = intent.getStringExtra(AppRouter.KEY_DATA)?.toUriOrNull() ?: throw FileNotFoundException()
		val sections =
			requireNotNull(intent.getSerializableExtraCompat<Array<BackupSection>>(AppRouter.KEY_ENTRIES)?.toSet())
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
			ZipInputStream(contentResolver.openInputStream(source)).use { input ->
				repository.restoreBackup(input, sections, progress)
			}
			progressUpdateJob?.cancelAndJoin()
			if (checkNotificationPermission(CHANNEL_ID)) {
				notificationManager.notify(notificationTag, startId, createResultNotification(source))
			}
		}
	}

	private fun IntentJobContext.buildNotification(progress: Progress): Notification {
		return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(getString(R.string.restoring_backup))
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

	private fun createResultNotification(uri: Uri): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
			.setContentText(getString(R.string.backup_saved))
			.setSmallIcon(R.drawable.ic_stat_done)
		val shareIntent = ShareCompat.IntentBuilder(this)
			.setStream(uri)
			.setType(contentResolver.getType(uri) ?: "application/zip")
			.setChooserTitle(R.string.share_backup)
			.createChooserIntent()
		notification.setContentIntent(
			PendingIntentCompat.getActivity(
				applicationContext,
				0,
				shareIntent,
				0,
				false,
			),
		)
		return notification.build()
	}

	companion object {

		private const val TAG = "RESTORE"
		private const val FOREGROUND_NOTIFICATION_ID = 39

		@CheckResult
		fun start(context: Context, uri: Uri, sections: Set<BackupSection>): Boolean = try {
			val intent = Intent(context, RestoreService::class.java)
			intent.putExtra(AppRouter.KEY_DATA, uri.toString())
			intent.putExtra(AppRouter.KEY_ENTRIES, sections.toTypedArray())
			ContextCompat.startForegroundService(context, intent)
			true
		} catch (e: Exception) {
			e.printStackTraceDebug()
			false
		}
	}
}
