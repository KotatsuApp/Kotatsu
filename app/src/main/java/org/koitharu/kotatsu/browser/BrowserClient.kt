package org.koitharu.kotatsu.browser

import android.graphics.Bitmap
import android.webkit.WebResourceResponse
import android.webkit.WebView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.network.WebViewClientCompat

class BrowserClient(private val callback: BrowserCallback) : WebViewClientCompat(), KoinComponent {

	private val okHttp by inject<OkHttpClient>()

	override fun onPageFinished(webView: WebView, url: String) {
		super.onPageFinished(webView, url)
		callback.onLoadingStateChanged(isLoading = false)
	}

	override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
		super.onPageStarted(view, url, favicon)
		callback.onLoadingStateChanged(isLoading = true)
	}

	override fun onPageCommitVisible(view: WebView, url: String?) {
		super.onPageCommitVisible(view, url)
		callback.onTitleChanged(view.title.orEmpty(), url)
	}

	override fun shouldInterceptRequestCompat(view: WebView, url: String): WebResourceResponse? {
		return runCatching {
			val request = Request.Builder()
				.url(url)
				.build()
			val response = okHttp.newCall(request).execute()
			val ct = response.body?.contentType()
			WebResourceResponse(
				"${ct?.type}/${ct?.subtype}",
				ct?.charset()?.name() ?: "utf-8",
				response.body?.byteStream()
			)
		}.getOrNull()
	}
}