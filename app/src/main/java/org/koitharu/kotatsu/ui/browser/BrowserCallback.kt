package org.koitharu.kotatsu.ui.browser

interface BrowserCallback {

	fun onLoadingStateChanged(isLoading: Boolean)

	fun onTitleChanged(title: CharSequence, subtitle: CharSequence?)
}