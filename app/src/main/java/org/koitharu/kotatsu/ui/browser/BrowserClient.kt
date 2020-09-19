package org.koitharu.kotatsu.ui.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.utils.ext.safe

class BrowserClient(private val callback: BrowserCallback) : WebViewClient(), KoinComponent {

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

	override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

	override fun shouldOverrideUrlLoading(view: WebView, url: String) = false

	override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
		return url?.let(::doRequest)
	}

	override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
		return request?.url?.toString()?.let(::doRequest)
	}

	private fun doRequest(url: String): WebResourceResponse? = safe {
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
	}
}