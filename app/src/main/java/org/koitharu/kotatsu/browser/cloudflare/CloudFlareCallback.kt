package org.koitharu.kotatsu.browser.cloudflare

interface CloudFlareCallback {

	fun onPageLoaded()

	fun onCheckPassed()
}