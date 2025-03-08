package org.koitharu.kotatsu.browser

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import org.koitharu.kotatsu.core.network.proxy.ProxyProvider

open class BrowserClient(
	private val proxyProvider: ProxyProvider,
	private val callback: BrowserCallback
) : WebViewClientCompat() {

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
}
