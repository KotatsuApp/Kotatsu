package org.koitharu.kotatsu.browser.cloudflare

import android.graphics.Bitmap
import android.webkit.WebView
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.core.network.AndroidCookieJar
import org.koitharu.kotatsu.core.network.WebViewClientCompat

class CloudFlareClient(
	private val cookieJar: AndroidCookieJar,
	private val callback: CloudFlareCallback,
	private val targetUrl: String
) : WebViewClientCompat() {

	private val oldClearance = getCookieValue(CF_CLEARANCE)

	override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
		super.onPageStarted(view, url, favicon)
		checkClearance()
	}

	override fun onPageCommitVisible(view: WebView?, url: String?) {
		super.onPageCommitVisible(view, url)
		callback.onPageLoaded()
	}

	override fun onPageFinished(view: WebView?, url: String?) {
		super.onPageFinished(view, url)
		callback.onPageLoaded()
	}

	private fun checkClearance() {
		val clearance = getCookieValue(CF_CLEARANCE)
		if (clearance != null && clearance != oldClearance) {
			callback.onCheckPassed()
		}
	}

	private fun getCookieValue(name: String): String? {
		return cookieJar.loadForRequest(targetUrl.toHttpUrl())
			.find { it.name == name }?.value
	}

	private companion object {

		const val CF_CLEARANCE = "cf_clearance"
	}
}