package org.koitharu.kotatsu.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.core.github.GithubRepository
import org.koitharu.kotatsu.core.github.VersionId
import org.koitharu.kotatsu.core.prefs.AppSettings
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

class AppUpdateChecker(private val activity: ComponentActivity) {

	private val settings by activity.inject<AppSettings>()
	private val repo by activity.inject<GithubRepository>()

	operator fun invoke() {
		if (isUpdateSupported(activity) && settings.appUpdateAuto && settings.appUpdate + PERIOD < System.currentTimeMillis()) {
			launch()
		}
	}

	private fun launch() = activity.lifecycleScope.launch(Dispatchers.Main) {
		try {
			val version = repo.getLatestVersion()
			val newVersionId = VersionId.parse(version.name)
			val currentVersionId = VersionId.parse(BuildConfig.VERSION_NAME)
			if (newVersionId > currentVersionId) {
				showUpdateDialog(version)
			}
			settings.appUpdate = System.currentTimeMillis()
		} catch (_: CancellationException) {
		} catch (e: Throwable) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
		}
	}

	private fun showUpdateDialog(version: AppVersion) {
		MaterialAlertDialogBuilder(activity)
			.setTitle(R.string.app_update_available)
			.setMessage(buildString {
				append(activity.getString(R.string.new_version_s, version.name))
				appendLine()
				append(
					activity.getString(
						R.string.size_s,
						FileSizeUtils.formatBytes(activity, version.apkSize)
					)
				)
				appendLine()
				appendLine()
				append(version.description)
			})
			.setPositiveButton(R.string.download) { _, _ ->
				activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(version.apkUrl)))
			}
			.setNegativeButton(R.string.close, null)
			.create()
			.show()
	}

	companion object {

		private const val CERT_SHA1 = "2C:19:C7:E8:07:61:2B:8E:94:51:1B:FD:72:67:07:64:5D:C2:58:AE"
		private val PERIOD = TimeUnit.HOURS.toMillis(6)

		fun isUpdateSupported(context: Context): Boolean {
			return getCertificateSHA1Fingerprint(context) == CERT_SHA1
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
				val publicKey: ByteArray = md.digest(c.encoded)
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