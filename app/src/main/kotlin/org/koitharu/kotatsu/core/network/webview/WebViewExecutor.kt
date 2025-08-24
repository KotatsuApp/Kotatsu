package org.koitharu.kotatsu.core.network.webview

import android.content.Context
import android.webkit.WebView
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.ext.configureForParser
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.sanitizeHeaderValue
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class WebViewExecutor @Inject constructor(
	@ApplicationContext private val context: Context
) {

	private var webViewCached: WeakReference<WebView>? = null
	private val mutex = Mutex()

	suspend fun evaluateJs(baseUrl: String?, script: String): String? = mutex.withLock {
		withContext(Dispatchers.Main.immediate) {
			val webView = obtainWebView()
			if (!baseUrl.isNullOrEmpty()) {
				suspendCoroutine { cont ->
					webView.webViewClient = ContinuationResumeWebViewClient(cont)
					webView.loadDataWithBaseURL(baseUrl, " ", "text/html", null, null)
				}
			}
			suspendCoroutine { cont ->
				webView.evaluateJavascript(script) { result ->
					cont.resume(result?.takeUnless { it == "null" })
				}
			}
		}
	}

	@MainThread
	fun getDefaultUserAgent() = runCatching {
		obtainWebView().settings.userAgentString.sanitizeHeaderValue().trim().nullIfEmpty()
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrNull()

	@MainThread
	private fun obtainWebView(): WebView = webViewCached?.get() ?: WebView(context).also {
		it.configureForParser(null)
		webViewCached = WeakReference(it)
	}
}
