package org.koitharu.kotatsu.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.core.network.webview.adblock.AdBlock
import java.io.ByteArrayInputStream

open class BrowserClient(
	private val callback: BrowserCallback,
	private val adBlock: AdBlock?,
) : WebViewClient() {

	/**
	 * https://stackoverflow.com/questions/57414530/illegalstateexception-reasonphrase-cant-be-empty-with-android-webview
	 */

	override fun onPageFinished(webView: WebView, url: String) {
		super.onPageFinished(webView, url)
		callback.onLoadingStateChanged(isLoading = false)
	}

	override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
		super.onPageStarted(view, url, favicon)
		callback.onLoadingStateChanged(isLoading = true)
	}

	override fun onPageCommitVisible(view: WebView, url: String) {
		super.onPageCommitVisible(view, url)
		callback.onTitleChanged(view.title.orEmpty(), url)
	}

	override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
		super.doUpdateVisitedHistory(view, url, isReload)
		callback.onHistoryChanged()
	}

	@WorkerThread
	@Deprecated("Deprecated in Java")
	override fun shouldInterceptRequest(
		view: WebView?,
		url: String?
	): WebResourceResponse? = if (url.isNullOrEmpty() || adBlock?.shouldLoadUrl(url, view?.getUrlSafe()) ?: true) {
		super.shouldInterceptRequest(view, url)
	} else {
		emptyResponse()
	}

	@WorkerThread
	override fun shouldInterceptRequest(
		view: WebView?,
		request: WebResourceRequest?
	): WebResourceResponse? =
		if (request == null || adBlock?.shouldLoadUrl(request.url.toString(), view?.getUrlSafe()) ?: true) {
			super.shouldInterceptRequest(view, request)
		} else {
			emptyResponse()
		}

	private fun emptyResponse(): WebResourceResponse =
		WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))

	@SuppressLint("WrongThread")
	@AnyThread
	private fun WebView.getUrlSafe(): String? = if (Looper.myLooper() == Looper.getMainLooper()) {
		url
	} else {
		runBlocking(Dispatchers.Main.immediate) {
			url
		}
	}
}
