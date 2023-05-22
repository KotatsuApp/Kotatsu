package org.koitharu.kotatsu.browser

import android.webkit.WebView
import androidx.activity.OnBackPressedCallback

class WebViewBackPressedCallback(
	private val webView: WebView,
) : OnBackPressedCallback(false), OnHistoryChangedListener {

	init {
		onHistoryChanged()
	}

	override fun handleOnBackPressed() {
		webView.goBack()
	}

	override fun onHistoryChanged() {
		isEnabled = webView.canGoBack()
	}
}
