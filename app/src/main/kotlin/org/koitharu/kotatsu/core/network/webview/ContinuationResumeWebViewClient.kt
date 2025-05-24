package org.koitharu.kotatsu.core.network.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ContinuationResumeWebViewClient(
	private val continuation: Continuation<Unit>,
) : WebViewClient() {

	override fun onPageFinished(view: WebView?, url: String?) {
		view?.webViewClient = WebViewClient() // reset to default
		continuation.resume(Unit)
	}
}
