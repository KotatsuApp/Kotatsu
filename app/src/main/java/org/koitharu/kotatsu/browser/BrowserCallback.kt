package org.koitharu.kotatsu.browser

interface BrowserCallback {

	fun onLoadingStateChanged(isLoading: Boolean)

	fun onTitleChanged(title: CharSequence, subtitle: CharSequence?)
}