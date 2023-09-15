package org.koitharu.kotatsu.settings.about

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug


class UpdateDownloadReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		when (intent.action) {
			DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
				val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
				if (downloadId == 0L) {
					return
				}
				val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

				@Suppress("DEPRECATION")
				val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
				installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
				installIntent.setDataAndType(
					dm.getUriForDownloadedFile(downloadId),
					dm.getMimeTypeForDownloadedFile(downloadId),
				)
				installIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
				try {
					context.startActivity(installIntent)
				} catch (e: ActivityNotFoundException) {
					e.printStackTraceDebug()
				}
			}
		}
	}
}
