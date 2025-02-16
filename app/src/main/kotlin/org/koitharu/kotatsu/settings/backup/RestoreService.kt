package org.koitharu.kotatsu.settings.backup

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ErrorReporterReceiver
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipInput
import org.koitharu.kotatsu.core.backup.CompositeResult
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getFileDisplayName
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.core.util.progress.Progress
import org.koitharu.kotatsu.parsers.util.mapToArray
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import java.io.File
import java.io.FileNotFoundException
import java.util.EnumSet
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class RestoreService : CoroutineIntentService() {

	@Inject
	lateinit var repository: BackupRepository

	private lateinit var notificationManager: NotificationManagerCompat

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(applicationContext)
	}

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		startForeground(this)
		val uri = intent.getStringExtra(AppRouter.KEY_DATA)?.toUriOrNull() ?: throw FileNotFoundException()
		val displayName = contentResolver.getFileDisplayName(uri)
		val entries = intent.getIntArrayExtra(AppRouter.KEY_ENTRIES)
			?.mapTo(EnumSet.noneOf(BackupEntry.Name::class.java)) { BackupEntry.Name.entries[it] }
		if (entries.isNullOrEmpty()) {
			throw IllegalArgumentException("No entries specified")
		}
		val result = runInterruptible(Dispatchers.IO) {
			val tempFile = File.createTempFile("backup_", ".tmp")
			(contentResolver.openInputStream(uri) ?: throw FileNotFoundException()).use { input ->
				tempFile.outputStream().use { output ->
					input.copyTo(output)
				}
			}
			BackupZipInput.from(tempFile)
		}.use { backupInput ->
			restoreImpl(displayName, backupInput, entries)
		}
		if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			val notification = buildNotification(displayName, result)
			notificationManager.notify(TAG, startId, notification)
		}
	}

	override fun IntentJobContext.onError(error: Throwable) {
		if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			val result = CompositeResult()
			result += error
			val notification = buildNotification(null, result)
			notificationManager.notify(TAG, startId, notification)
		}
	}

	private suspend fun IntentJobContext.restoreImpl(
		displayName: String?,
		input: BackupZipInput,
		entries: Set<BackupEntry.Name>
	): CompositeResult {
		val result = CompositeResult()
		val showNotification = applicationContext.checkNotificationPermission(CHANNEL_ID)
		var progress = Progress(0, entries.size)

		fun notify(childProgress: Progress? = null) {
			if (showNotification) {
				val p = childProgress?.let { progress + it } ?: progress
				notificationManager.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(displayName, p))
			}
		}

		notify()

		if (BackupEntry.Name.HISTORY in entries) {
			input.getEntry(BackupEntry.Name.HISTORY)?.let {
				flow {
					result += repository.restoreHistory(it, this)
				}.collect { p ->
					notify(p)
				}
			}
			progress++
		}

		notify()

		if (BackupEntry.Name.CATEGORIES in entries) {
			input.getEntry(BackupEntry.Name.CATEGORIES)?.let {
				result += repository.restoreCategories(it)
			}
			progress++
		}

		notify()

		if (BackupEntry.Name.FAVOURITES in entries) {
			input.getEntry(BackupEntry.Name.FAVOURITES)?.let {
				flow {
					result += repository.restoreFavourites(it, this)
				}.collect { p ->
					notify(p)
				}
			}
		}

		notify()

		if (BackupEntry.Name.BOOKMARKS in entries) {
			input.getEntry(BackupEntry.Name.BOOKMARKS)?.let {
				result += repository.restoreBookmarks(it)
			}
			progress++
		}

		notify()

		if (BackupEntry.Name.SOURCES in entries) {
			input.getEntry(BackupEntry.Name.SOURCES)?.let {
				result += repository.restoreSources(it)
			}
			progress++
		}

		notify()

		if (BackupEntry.Name.SETTINGS in entries) {
			input.getEntry(BackupEntry.Name.SETTINGS)?.let {
				result += repository.restoreSettings(it)
			}
			progress++
		}

		notify()

		return result
	}

	@SuppressLint("InlinedApi")
	private fun startForeground(jobContext: IntentJobContext) {
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
			.setName(getString(R.string.restoring_backup))
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		notificationManager.createNotificationChannel(channel)

		val notification = jobContext.buildNotification(null, null)

		jobContext.setForeground(
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	}

	private fun IntentJobContext.buildNotification(fileName: String?, progress: Progress?): Notification {
		return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(getString(R.string.restoring_backup))
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setDefaults(0)
			.setSilent(true)
			.setOngoing(true)
			.setProgress(progress?.total ?: 0, progress?.progress ?: 0, progress == null)
			.setContentText(
				concatStrings(
					context = this@RestoreService,
					a = fileName,
					b = progress?.run { getString(R.string.percent_string_pattern, percentSting()) },
				),
			)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.addAction(
				materialR.drawable.material_ic_clear_black_24dp,
				applicationContext.getString(android.R.string.cancel),
				getCancelIntent(),
			).build()
	}

	private fun buildNotification(fileName: String?, result: CompositeResult): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
			.setSubText(fileName)

		when {
			result.isEmpty -> notification.setContentTitle(getString(R.string.data_not_restored))
				.setContentText(getString(R.string.data_not_restored_text))
				.setSmallIcon(android.R.drawable.stat_notify_error)

			result.isAllSuccess -> notification.setContentTitle(getString(R.string.data_restored))
				.setContentText(getString(R.string.data_restored_success))
				.setSmallIcon(R.drawable.ic_stat_done)

			result.isAllFailed -> notification.setContentTitle(getString(R.string.error))
				.setContentText(
					result.failures.map { it.getDisplayMessage(resources) }.distinct().joinToString("\n"),
				)
				.setSmallIcon(android.R.drawable.stat_notify_error)

			else -> notification.setContentTitle(getString(R.string.data_restored))
				.setContentText(getString(R.string.data_restored_with_errors))
				.setSmallIcon(R.drawable.ic_stat_done)
		}
		result.failures.firstOrNull()?.let { error ->
			ErrorReporterReceiver.getPendingIntent(applicationContext, error)?.let { reportIntent ->
				notification.addAction(
					R.drawable.ic_alert_outline,
					applicationContext.getString(R.string.report),
					reportIntent,
				)
			}
		}
		notification.setContentIntent(
			PendingIntentCompat.getActivity(
				applicationContext,
				0,
				AppRouter.homeIntent(this),
				0,
				false,
			),
		)
		return notification.build()
	}

	private fun concatStrings(context: Context, a: String?, b: String?): String? = when {
		a.isNullOrEmpty() && b.isNullOrEmpty() -> null
		a.isNullOrEmpty() -> b?.nullIfEmpty()
		b.isNullOrEmpty() -> a.nullIfEmpty()
		else -> context.getString(R.string.download_summary_pattern, a, b)
	}

	companion object {

		private const val TAG = "restore"
		private const val CHANNEL_ID = "restore_backup"
		private const val FOREGROUND_NOTIFICATION_ID = 39

		fun start(context: Context, uri: Uri, entries: Set<BackupEntry.Name>): Boolean = try {
			val intent = Intent(context, RestoreService::class.java)
			intent.putExtra(AppRouter.KEY_DATA, uri.toString())
			intent.putExtra(AppRouter.KEY_ENTRIES, entries.mapToArray { it.ordinal }.toIntArray())
			ContextCompat.startForegroundService(context, intent)
			true
		} catch (e: Exception) {
			e.printStackTraceDebug()
			false
		}
	}
}
