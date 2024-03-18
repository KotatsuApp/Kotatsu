package org.koitharu.kotatsu.browser.cloudflare

import org.koitharu.kotatsu.browser.BrowserCallback

interface CloudFlareCallback : BrowserCallback {

	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	override fun onTitleChanged(title: CharSequence, subtitle: CharSequence?) = Unit

	fun onPageLoaded()

	fun onCheckPassed()

	fun onLoopDetected()
}
