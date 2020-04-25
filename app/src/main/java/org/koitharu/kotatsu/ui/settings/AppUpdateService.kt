package org.koitharu.kotatsu.ui.settings

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.core.github.GithubRepository
import org.koitharu.kotatsu.core.github.VersionId
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.ui.common.BaseService
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.byte2HexFormatted
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit


class AppUpdateService : BaseService() {

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
				AppSettings(this@AppUpdateService).appUpdate = System.currentTimeMillis()
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
			append(FileSizeUtils.formatBytes(this@AppUpdateService, newVersion.apkSize))
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
		builder.addAction(
			R.drawable.ic_download, getString(R.string.download),
			PendingIntent.getActivity(
				this,
				NOTIFICATION_ID + 1,
				Intent(Intent.ACTION_VIEW, Uri.parse(newVersion.apkUrl)),
				PendingIntent.FLAG_CANCEL_CURRENT
			)
		)
		builder.setSmallIcon(R.drawable.ic_stat_update)
		builder.setAutoCancel(true)
		builder.color = ContextCompat.getColor(this, R.color.blue_primary_dark)
		builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
		manager.notify(NOTIFICATION_ID, builder.build())
	}

	companion object {

		private const val CERT_SHA1 = "2C:19:C7:E8:07:61:2B:8E:94:51:1B:FD:72:67:07:64:5D:C2:58:AE"
		private const val NOTIFICATION_ID = 202
		private const val CHANNEL_ID = "update"
		private val PERIOD = TimeUnit.HOURS.toMillis(6)

		@Suppress("UNUSED_PARAMETER")
		fun isUpdateSupported(context: Context) = false

		fun startIfRequired(context: Context) {
			if (!isUpdateSupported(context)) {
				return
			}
			val settings = AppSettings(context)
			if (settings.appUpdateAuto) {
				val lastUpdate = settings.appUpdate
				if (lastUpdate + PERIOD < System.currentTimeMillis()) {
					start(context)
				}
			}
		}

		private fun start(context: Context) {
			try {
				context.startService(Intent(context, AppUpdateService::class.java))
			} catch (_: IllegalStateException) {
			}
		}

		@Suppress("DEPRECATION")
		@SuppressLint("PackageManagerGetSignatures")
		private fun getCertificateSHA1Fingerprint(context: Context): String? {
			val packageInfo = try {
				context.packageManager.getPackageInfo(
					context.packageName,
					PackageManager.GET_SIGNATURES
				)
			} catch (e: PackageManager.NameNotFoundException) {
				e.printStackTrace()
				return null
			}
			val signatures = packageInfo?.signatures
			val cert: ByteArray = signatures?.firstOrNull()?.toByteArray() ?: return null
			val input: InputStream = ByteArrayInputStream(cert)
			val c = try {
				val cf = CertificateFactory.getInstance("X509")
				cf.generateCertificate(input) as X509Certificate
			} catch (e: CertificateException) {
				e.printStackTrace()
				return null
			}
			return try {
				val md: MessageDigest = MessageDigest.getInstance("SHA1")
				val publicKey: ByteArray = md.digest(c.getEncoded())
				publicKey.byte2HexFormatted()
			} catch (e: NoSuchAlgorithmException) {
				e.printStackTrace()
				null
			} catch (e: CertificateEncodingException) {
				e.printStackTrace()
				null
			}
		}
	}
}