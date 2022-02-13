package org.koitharu.kotatsu.utils

import android.app.DownloadManager
import android.app.DownloadManager.Request.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.utils.ext.toFileNameSafe
import java.io.File
import kotlin.coroutines.resume

class DownloadManagerHelper(
	private val context: Context,
	private val cookieJar: CookieJar,
) {

	private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
	private val subDir = context.getString(R.string.app_name).toFileNameSafe()

	fun downloadPage(page: MangaPage, fullUrl: String): Long {
		val uri = fullUrl.toUri()
		val cookies = cookieJar.loadForRequest(fullUrl.toHttpUrl())
		val dest = subDir + File.separator + uri.lastPathSegment
		val request = DownloadManager.Request(uri)
			.addRequestHeader(CommonHeaders.REFERER, page.referer)
			.addRequestHeader(CommonHeaders.COOKIE, cookieHeader(cookies))
			.setAllowedOverMetered(true)
			.setAllowedNetworkTypes(NETWORK_WIFI or NETWORK_MOBILE)
			.setNotificationVisibility(VISIBILITY_VISIBLE)
			.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, dest)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			@Suppress("DEPRECATION")
			request.allowScanningByMediaScanner()
		}
		return manager.enqueue(request)
	}

	suspend fun awaitDownload(id: Long): Uri {
		getUriForDownloadedFile(id)?.let { return it } // fast path
		suspendCancellableCoroutine<Unit> { cont ->
			val receiver = object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent?) {
					if (
						intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE &&
						intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L) == id
					) {
						context.unregisterReceiver(this)
						cont.resume(Unit)
					}
				}
			}
			context.registerReceiver(
				receiver,
				IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
			)
			cont.invokeOnCancellation {
				context.unregisterReceiver(receiver)
			}
		}
		return checkNotNull(getUriForDownloadedFile(id))
	}

	private suspend fun getUriForDownloadedFile(id: Long) = withContext(Dispatchers.IO) {
		manager.getUriForDownloadedFile(id)
	}

	private fun cookieHeader(cookies: List<Cookie>): String = buildString {
		cookies.forEachIndexed { index, cookie ->
			if (index > 0) append("; ")
			append(cookie.name).append('=').append(cookie.value)
		}
	}
}