package org.koitharu.kotatsu.browser.cloudflare

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.core.network.AndroidCookieJar

private const val CF_CLEARANCE = "cf_clearance"

class CloudFlareClient(
	private val cookieJar: AndroidCookieJar,
	private val callback: CloudFlareCallback,
	private val targetUrl: String,
) : WebViewClient() {

	private val oldClearance = getClearance()

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
		val clearance = getClearance()
		if (clearance != null && clearance != oldClearance) {
			callback.onCheckPassed()
		}
	}

	private fun getClearance(): String? {
		return cookieJar.loadForRequest(targetUrl.toHttpUrl())
			.find { it.name == CF_CLEARANCE }?.value
	}
}