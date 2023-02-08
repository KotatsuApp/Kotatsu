package org.koitharu.kotatsu.browser.cloudflare

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.core.view.isInvisible
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.Headers
import org.koitharu.kotatsu.base.ui.AlertDialogFragment
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.CommonHeadersInterceptor
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.databinding.FragmentCloudflareBinding
import org.koitharu.kotatsu.utils.ext.stringArgument
import org.koitharu.kotatsu.utils.ext.withArgs
import javax.inject.Inject

@AndroidEntryPoint
class CloudFlareDialog : AlertDialogFragment<FragmentCloudflareBinding>(), CloudFlareCallback {

	private val url by stringArgument(ARG_URL)
	private val pendingResult = Bundle(1)

	@Inject
	lateinit var cookieJar: MutableCookieJar

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentCloudflareBinding.inflate(inflater, container, false)

	@SuppressLint("SetJavaScriptEnabled")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		with(binding.webView.settings) {
			javaScriptEnabled = true
			cacheMode = WebSettings.LOAD_DEFAULT
			domStorageEnabled = true
			databaseEnabled = true
			userAgentString = arguments?.getString(ARG_UA) ?: CommonHeadersInterceptor.userAgentChrome
		}
		binding.webView.webViewClient = CloudFlareClient(cookieJar, this, url.orEmpty())
		CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
		if (url.isNullOrEmpty()) {
			dismissAllowingStateLoss()
		} else {
			binding.webView.loadUrl(url.orEmpty())
		}
	}

	override fun onDestroyView() {
		binding.webView.stopLoading()
		binding.webView.destroy()
		super.onDestroyView()
	}

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder).setNegativeButton(android.R.string.cancel, null)
	}

	override fun onResume() {
		super.onResume()
		binding.webView.onResume()
	}

	override fun onPause() {
		binding.webView.onPause()
		super.onPause()
	}

	override fun onDismiss(dialog: DialogInterface) {
		setFragmentResult(TAG, pendingResult)
		super.onDismiss(dialog)
	}

	override fun onPageLoaded() {
		bindingOrNull()?.progressBar?.isInvisible = true
	}

	override fun onCheckPassed() {
		pendingResult.putBoolean(EXTRA_RESULT, true)
		dismissAllowingStateLoss()
	}

	companion object {

		const val TAG = "CloudFlareDialog"
		const val EXTRA_RESULT = "result"
		private const val ARG_URL = "url"
		private const val ARG_UA = "ua"

		fun newInstance(url: String, headers: Headers?) = CloudFlareDialog().withArgs(2) {
			putString(ARG_URL, url)
			headers?.get(CommonHeaders.USER_AGENT)?.let {
				putString(ARG_UA, it)
			}
		}
	}
}
