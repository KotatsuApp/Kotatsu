package org.koitharu.kotatsu.core.network.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

open class ContinuationResumeWebViewClient(
	private val continuation: Continuation<Unit>,
) : WebViewClient() {

	override fun onPageFinished(view: WebView?, url: String?) {
		resumeContinuation(view)
	}

	protected fun resumeContinuation(view: WebView?) {
		if (continuation !is CancellableContinuation || continuation.isActive) {
			view?.webViewClient = WebViewClient() // reset to default
			continuation.resume(Unit)
		}
	}
}
