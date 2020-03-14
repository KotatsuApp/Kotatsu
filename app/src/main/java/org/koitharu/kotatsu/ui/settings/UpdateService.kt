package org.koitharu.kotatsu.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.core.github.GithubRepository
import org.koitharu.kotatsu.core.github.VersionId
import org.koitharu.kotatsu.ui.common.BaseService
import org.koitharu.kotatsu.utils.FileSizeUtils
import java.util.concurrent.TimeUnit

class UpdateService : BaseService() {

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		launch(Dispatchers.IO) {
			try {
				val repo = GithubRepository()
				val version = repo.getLatestVersion()
				val newVersionId = VersionId.parse(version.name)
				val currentVersionId = VersionId.parse(BuildConfig.VERSION_NAME)
				if (newVersionId > currentVersionId) {
					withContext(Dispatchers.Main) {
						showUpdateNotification(version)
					}
				}
				PreferenceManager.getDefaultSharedPreferences(this@UpdateService).edit(true) {
					putLong(getString(R.string.key_app_update), System.currentTimeMillis())
				}
			} catch (_: CancellationException) {
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
			} finally {
				withContext(Dispatchers.Main) {
					stopSelf(startId)
				}
			}
		}
		return START_NOT_STICKY
	}

	private fun showUpdateNotification(newVersion: AppVersion) {
		val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
			&& manager.getNotificationChannel(CHANNEL_ID) == null
		) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				getString(R.string.application_update),
				NotificationManager.IMPORTANCE_DEFAULT
			)
			manager.createNotificationChannel(channel)
		}
		val builder = NotificationCompat.Builder(this, CHANNEL_ID)
		builder.setContentTitle(getString(R.string.app_update_available))
		builder.setContentText(buildString {
			append(newVersion.name)
			append(" (")
			append(FileSizeUtils.formatBytes(this@UpdateService, newVersion.apkSize))
			append(')')
		})
		builder.setContentIntent(
			PendingIntent.getActivity(
				this,
				NOTIFICATION_ID,
				Intent(Intent.ACTION_VIEW, Uri.parse(newVersion.url)),
				PendingIntent.FLAG_CANCEL_CURRENT
			)
		)
		builder.setSmallIcon(R.drawable.ic_stat_update)
		builder.setAutoCancel(true)
		builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
		manager.notify(NOTIFICATION_ID, builder.build())
	}

	companion object {

		private const val NOTIFICATION_ID = 202
		private const val CHANNEL_ID = "update"
		private val PERIOD = TimeUnit.HOURS.toMillis(10)

		fun start(context: Context) =
			context.startService(Intent(context, UpdateService::class.java))

		fun startIfRequired(context: Context) {
			val lastUpdate = PreferenceManager.getDefaultSharedPreferences(context)
				.getLong(context.getString(R.string.key_app_update), 0)
			if (lastUpdate + PERIOD < System.currentTimeMillis()) {
				start(context)
			}
		}
	}
}