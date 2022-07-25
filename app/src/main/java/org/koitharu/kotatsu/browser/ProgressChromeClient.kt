package org.koitharu.kotatsu.browser

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.core.view.isVisible
import org.koitharu.kotatsu.utils.ext.setProgressCompat

private const val PROGRESS_MAX = 100

class ProgressChromeClient(
	private val progressIndicator: ProgressBar,
) : WebChromeClient() {

	init {
		progressIndicator.max = PROGRESS_MAX
	}

	override fun onProgressChanged(view: WebView?, newProgress: Int) {
		super.onProgressChanged(view, newProgress)
		if (!progressIndicator.isVisible) {
			return
		}
		if (newProgress in 1 until PROGRESS_MAX) {
			progressIndicator.isIndeterminate = false
			progressIndicator.setProgressCompat(newProgress.coerceAtMost(PROGRESS_MAX), true)
		} else {
			progressIndicator.isIndeterminate = true
		}
	}
}