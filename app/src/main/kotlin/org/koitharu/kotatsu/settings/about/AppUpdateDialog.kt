package org.koitharu.kotatsu.settings.about

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.Markwon
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage

class AppUpdateDialog(private val activity: AppCompatActivity) {

	private lateinit var latestVersion: AppVersion

	private val permissionRequest = activity.registerForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) {
		if (it) {
			downloadUpdateImpl()
		} else {
			openInBrowser()
		}
	}

	fun show(version: AppVersion) {
		latestVersion = version
		val message = buildSpannedString {
			append(activity.getString(R.string.new_version_s, version.name))
			appendLine()
			append(activity.getString(R.string.size_s, FileSize.BYTES.format(activity, version.apkSize)))
			appendLine()
			appendLine()
			append(Markwon.create(activity).toMarkdown(version.description))
		}
		MaterialAlertDialogBuilder(activity, DIALOG_THEME_CENTERED)
			.setTitle(R.string.app_update_available)
			.setMessage(message)
			.setIcon(R.drawable.ic_app_update)
			.setNeutralButton(R.string.open_in_browser) { _, _ ->
				val intent = Intent(Intent.ACTION_VIEW, version.url.toUri())
				activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.open_in_browser)))
			}.setPositiveButton(R.string.update) { _, _ ->
				downloadUpdate()
			}.setNegativeButton(android.R.string.cancel, null)
			.setCancelable(false)
			.create()
			.show()
	}

	private fun downloadUpdate() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			permissionRequest.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		} else {
			downloadUpdateImpl()
		}
	}

	private fun downloadUpdateImpl() = runCatching {
		val version = latestVersion
		val url = version.apkUrl.toUri()
		val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val request = DownloadManager.Request(url)
			.setTitle("${activity.getString(R.string.app_name)} v${version.name}")
			.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.lastPathSegment)
			.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
			.setMimeType("application/vnd.android.package-archive")
		dm.enqueue(request)
	}.onSuccess {
		Toast.makeText(activity, R.string.download_started, Toast.LENGTH_SHORT).show()
	}.onFailure { e ->
		Toast.makeText(activity, e.getDisplayMessage(activity.resources), Toast.LENGTH_SHORT).show()
	}

	private fun openInBrowser() {
		val intent = Intent(Intent.ACTION_VIEW, latestVersion.url.toUri())
		activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.open_in_browser)))
	}
}
