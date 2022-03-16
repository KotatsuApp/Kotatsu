package org.koitharu.kotatsu.core.parser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebView
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.core.network.AndroidCookieJar
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.toList
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MangaLoaderContextImpl(
	override val httpClient: OkHttpClient,
	override val cookieJar: AndroidCookieJar,
	private val androidContext: Context,
) : MangaLoaderContext() {

	@SuppressLint("SetJavaScriptEnabled")
	override suspend fun evaluateJs(script: String): String? = withContext(Dispatchers.Main) {
		val webView = WebView(androidContext)
		webView.settings.javaScriptEnabled = true
		suspendCoroutine { cont ->
			webView.evaluateJavascript(script) { result ->
				cont.resume(result?.takeUnless { it == "null" })
			}
		}
	}

	override fun getConfig(source: MangaSource): MangaSourceConfig {
		return SourceSettings(androidContext, source)
	}

	override fun encodeBase64(data: ByteArray): String {
		return Base64.encodeToString(data, Base64.NO_PADDING)
	}

	override fun decodeBase64(data: String): ByteArray {
		return Base64.decode(data, Base64.DEFAULT)
	}

	override fun getPreferredLocales(): List<Locale> {
		return LocaleListCompat.getAdjustedDefault().toList()
	}
}