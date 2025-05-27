package org.koitharu.kotatsu.core.parser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import org.koitharu.kotatsu.core.image.BitmapDecoderCompat
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.network.webview.ContinuationResumeWebViewClient
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.core.util.ext.configureForParser
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.sanitizeHeaderValue
import org.koitharu.kotatsu.core.util.ext.toList
import org.koitharu.kotatsu.core.util.ext.toMimeType
import org.koitharu.kotatsu.core.util.ext.use
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.util.map
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class MangaLoaderContextImpl @Inject constructor(
	@MangaHttpClient override val httpClient: OkHttpClient,
	override val cookieJar: MutableCookieJar,
	@ApplicationContext private val androidContext: Context,
) : MangaLoaderContext() {

	private var webViewCached: WeakReference<WebView>? = null
	private val webViewUserAgent by lazy { obtainWebViewUserAgent() }
	private val jsMutex = Mutex()
	private val jsTimeout = TimeUnit.SECONDS.toMillis(4)

	@Deprecated("Provide a base url")
	@SuppressLint("SetJavaScriptEnabled")
	override suspend fun evaluateJs(script: String): String? = evaluateJs("", script)

	override suspend fun evaluateJs(baseUrl: String, script: String): String? = withTimeout(jsTimeout) {
		jsMutex.withLock {
			withContext(Dispatchers.Main.immediate) {
				val webView = obtainWebView()
				if (baseUrl.isNotEmpty()) {
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
	}

	override fun getDefaultUserAgent(): String = webViewUserAgent

	override fun getConfig(source: MangaSource): MangaSourceConfig {
		return SourceSettings(androidContext, source)
	}

	override fun encodeBase64(data: ByteArray): String {
		return Base64.encodeToString(data, Base64.NO_WRAP)
	}

	override fun decodeBase64(data: String): ByteArray {
		return Base64.decode(data, Base64.DEFAULT)
	}

	override fun getPreferredLocales(): List<Locale> {
		return LocaleListCompat.getAdjustedDefault().toList()
	}

	override fun redrawImageResponse(response: Response, redraw: (image: Bitmap) -> Bitmap): Response {
		return response.map { body ->
			BitmapDecoderCompat.decode(body.byteStream(), body.contentType()?.toMimeType(), isMutable = true)
				.use { bitmap ->
					(redraw(BitmapWrapper.create(bitmap)) as BitmapWrapper).use { result ->
						Buffer().also {
							result.compressTo(it.outputStream())
						}.asResponseBody("image/jpeg".toMediaType())
					}
				}
		}
	}

	override fun createBitmap(width: Int, height: Int): Bitmap = BitmapWrapper.create(width, height)

	@MainThread
	private fun obtainWebView(): WebView = webViewCached?.get() ?: WebView(androidContext).also {
		it.configureForParser(null)
		webViewCached = WeakReference(it)
	}

	private fun obtainWebViewUserAgent(): String {
		val mainDispatcher = Dispatchers.Main.immediate
		return if (!mainDispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
			obtainWebViewUserAgentImpl()
		} else {
			runBlocking(mainDispatcher) {
				obtainWebViewUserAgentImpl()
			}
		}
	}

	@MainThread
	private fun obtainWebViewUserAgentImpl() = runCatching {
		obtainWebView().settings.userAgentString.sanitizeHeaderValue()
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrDefault(UserAgents.FIREFOX_MOBILE)
}
