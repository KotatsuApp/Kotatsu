package org.koitharu.kotatsu.browser.cloudflare

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class CloudFlareClient(
	private val callback: CloudFlareCallback,
	private val targetUrl: String
) : WebViewClient(), KoinComponent {

	private val cookieJar = get<CookieJar>()
	private val cookieManager = CookieManager.getInstance()

	init {
		cookieManager.removeAllCookies(null)
	}

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
		val httpUrl = targetUrl.toHttpUrl()
		val cookies = cookieManager.getCookie(targetUrl).split(';').mapNotNull {
			Cookie.parse(httpUrl, it)
		}
		if (cookies.none { it.name == CF_CLEARANCE }) {
			return
		}
		cookieJar.saveFromResponse(httpUrl, cookies)
		callback.onCheckPassed()
	}

	private companion object {

		const val CF_UID = "__cfduid"
		const val CF_CLEARANCE = "cf_clearance"
	}
}