package org.koitharu.kotatsu.browser.cloudflare

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_cloudflare.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.network.UserAgentInterceptor
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class CloudFlareDialog : AlertDialogFragment(R.layout.fragment_cloudflare), CloudFlareCallback {

	private val url by stringArgument(ARG_URL)

	@SuppressLint("SetJavaScriptEnabled")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		with(webView.settings) {
			javaScriptEnabled = true
			cacheMode = WebSettings.LOAD_DEFAULT
			domStorageEnabled = true
			databaseEnabled = true
			userAgentString = UserAgentInterceptor.userAgent
		}
		webView.webViewClient = CloudFlareClient(this, url.orEmpty())
		CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
		if (url.isNullOrEmpty()) {
			dismissAllowingStateLoss()
		} else {
			webView.loadUrl(url.orEmpty())
		}
	}

	override fun onDestroyView() {
		webView.stopLoading()
		super.onDestroyView()
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setNegativeButton(android.R.string.cancel, null)
	}

	override fun onResume() {
		super.onResume()
		webView.onResume()
	}

	override fun onPause() {
		webView.onPause()
		super.onPause()
	}

	override fun onPageLoaded() {
		progressBar?.isInvisible = true
	}

	override fun onCheckPassed() {
		((parentFragment ?: activity) as? SwipeRefreshLayout.OnRefreshListener)?.onRefresh()
		dismiss()
	}

	companion object {

		const val TAG = "CloudFlareDialog"
		private const val ARG_URL = "url"

		fun newInstance(url: String) = CloudFlareDialog().withArgs(1) {
			putString(ARG_URL, url)
		}
	}
}