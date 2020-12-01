package org.koitharu.kotatsu.browser.cloudflare

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.network.UserAgentInterceptor
import org.koitharu.kotatsu.databinding.FragmentCloudflareBinding
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs

class CloudFlareDialog : AlertDialogFragment<FragmentCloudflareBinding>(), CloudFlareCallback {

	private val url by stringArgument(ARG_URL)

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentCloudflareBinding.inflate(inflater, container, false)

	@SuppressLint("SetJavaScriptEnabled")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		with(binding.webView.settings) {
			javaScriptEnabled = true
			cacheMode = WebSettings.LOAD_DEFAULT
			domStorageEnabled = true
			databaseEnabled = true
			userAgentString = UserAgentInterceptor.userAgent
		}
		binding.webView.webViewClient = CloudFlareClient(this, url.orEmpty())
		CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
		if (url.isNullOrEmpty()) {
			dismissAllowingStateLoss()
		} else {
			binding.webView.loadUrl(url.orEmpty())
		}
	}

	override fun onDestroyView() {
		binding.webView.stopLoading()
		super.onDestroyView()
	}

	override fun onBuildDialog(builder: AlertDialog.Builder) {
		builder.setNegativeButton(android.R.string.cancel, null)
	}

	override fun onResume() {
		super.onResume()
		binding.webView.onResume()
	}

	override fun onPause() {
		binding.webView.onPause()
		super.onPause()
	}

	override fun onPageLoaded() {
		binding.progressBar.isInvisible = true
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