package org.koitharu.kotatsu.settings.about

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.net.toUri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.requireValue
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
	private val repository: AppUpdateRepository,
	@ApplicationContext context: Context,
) : BaseViewModel() {

	val nextVersion = repository.observeAvailableUpdate()
	val downloadProgress = MutableStateFlow(-1f)
	val downloadState = MutableStateFlow(DownloadManager.STATUS_PENDING)
	val installIntent = MutableStateFlow<Intent?>(null)
	val onDownloadDone = MutableEventFlow<Intent>()

	private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
	private val appName = context.getString(R.string.app_name)

	init {
		if (nextVersion.value == null) {
			launchLoadingJob(Dispatchers.Default) {
				repository.fetchUpdate()
			}
		}
	}

	fun startDownload() {
		launchLoadingJob(Dispatchers.Default) {
			val version = nextVersion.requireValue()
			val url = version.apkUrl.toUri()
			val request = DownloadManager.Request(url)
				.setTitle("$appName v${version.name}")
				.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.lastPathSegment)
				.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
				.setMimeType("application/vnd.android.package-archive")
			val downloadId = downloadManager.enqueue(request)
			observeDownload(downloadId)
		}
	}

	fun onDownloadComplete(intent: Intent) {
		launchLoadingJob(Dispatchers.Default) {
			val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
			if (downloadId == 0L) {
				return@launchLoadingJob
			}
			@Suppress("DEPRECATION")
			val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
			installerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
			installerIntent.setDataAndType(
				downloadManager.getUriForDownloadedFile(downloadId),
				downloadManager.getMimeTypeForDownloadedFile(downloadId),
			)
			installerIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
			installIntent.value = installerIntent
			onDownloadDone.call(installerIntent)
		}
	}

	private suspend fun observeDownload(id: Long) {
		val query = DownloadManager.Query()
		query.setFilterById(id)
		while (coroutineContext.isActive) {
			downloadManager.query(query).use { cursor ->
				if (cursor.moveToFirst()) {
					val bytesDownloaded = cursor.getLong(
						cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
					)
					val bytesTotal = cursor.getLong(
						cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
					)
					downloadProgress.value = bytesDownloaded.toFloat() / bytesTotal
					val state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
					downloadState.value = state
					if (state == DownloadManager.STATUS_SUCCESSFUL || state == DownloadManager.STATUS_FAILED) {
						return
					}
				}
			}
			delay(100)
		}
	}
}
