package org.koitharu.kotatsu.settings.about

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.Markwon
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.core.util.FileSize
import com.google.android.material.R as materialR

class AppUpdateDialog(private val context: Context) {

	fun show(version: AppVersion) {
		val message = buildSpannedString {
			append(context.getString(R.string.new_version_s, version.name))
			appendLine()
			append(context.getString(R.string.size_s, FileSize.BYTES.format(context, version.apkSize)))
			appendLine()
			appendLine()
			append(Markwon.create(context).toMarkdown(version.description))
		}
		MaterialAlertDialogBuilder(
			context,
			materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered,
		)
			.setTitle(R.string.app_update_available)
			.setMessage(message)
			.setIcon(R.drawable.ic_app_update)
			.setNeutralButton(R.string.open_in_browser) { _, _ ->
				val intent = Intent(Intent.ACTION_VIEW, version.url.toUri())
				context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_in_browser)))
			}.setPositiveButton(R.string.update) { _, _ ->
				downloadUpdate(version)
			}.setNegativeButton(android.R.string.cancel, null)
			.setCancelable(false)
			.create()
			.show()
	}

	private fun downloadUpdate(version: AppVersion) {
		val url = version.apkUrl.toUri()
		val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
		val request = DownloadManager.Request(url)
			.setTitle("${context.getString(R.string.app_name)} v${version.name}")
			.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.lastPathSegment)
			.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
			.setMimeType("application/vnd.android.package-archive")
		dm.enqueue(request)
		Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
	}
}
