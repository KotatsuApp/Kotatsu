package org.koitharu.kotatsu.browser.cloudflare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.WebViewBackPressedCallback
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.CommonHeadersInterceptor
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.TaggedActivityResult
import org.koitharu.kotatsu.core.util.ext.catchingWebViewUnavailability
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class CloudFlareActivity : BaseActivity<ActivityBrowserBinding>(), CloudFlareCallback {

	private var pendingResult = RESULT_CANCELED

	@Inject
	lateinit var cookieJar: MutableCookieJar

	private var onBackPressedCallback: WebViewBackPressedCallback? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (!catchingWebViewUnavailability { setContentView(ActivityBrowserBinding.inflate(layoutInflater)) }) {
			return
		}
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		val url = intent?.dataString.orEmpty()
		with(viewBinding.webView.settings) {
			javaScriptEnabled = true
			cacheMode = WebSettings.LOAD_DEFAULT
			domStorageEnabled = true
			databaseEnabled = true
			userAgentString = intent?.getStringExtra(ARG_UA) ?: CommonHeadersInterceptor.userAgentFallback
		}
		viewBinding.webView.webViewClient = CloudFlareClient(cookieJar, this, url)
		onBackPressedCallback = WebViewBackPressedCallback(viewBinding.webView).also {
			onBackPressedDispatcher.addCallback(it)
		}
		CookieManager.getInstance().setAcceptThirdPartyCookies(viewBinding.webView, true)
		if (savedInstanceState != null) {
			return
		}
		if (url.isEmpty()) {
			finishAfterTransition()
		} else {
			onTitleChanged(getString(R.string.loading_), url)
			viewBinding.webView.loadUrl(url)
		}
	}

	override fun onDestroy() {
		viewBinding.webView.run {
			stopLoading()
			destroy()
		}
		super.onDestroy()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		viewBinding.webView.saveState(outState)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		viewBinding.webView.restoreState(savedInstanceState)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.appbar.updatePadding(
			top = insets.top,
		)
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			viewBinding.webView.stopLoading()
			finishAfterTransition()
			true
		}

		else -> super.onOptionsItemSelected(item)
	}

	override fun onResume() {
		super.onResume()
		viewBinding.webView.onResume()
	}

	override fun onPause() {
		viewBinding.webView.onPause()
		super.onPause()
	}

	override fun finish() {
		setResult(pendingResult)
		super.finish()
	}

	override fun onPageLoaded() {
		viewBinding.progressBar.isInvisible = true
	}

	override fun onCheckPassed() {
		pendingResult = RESULT_OK
		finishAfterTransition()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.progressBar.isVisible = isLoading
	}

	override fun onHistoryChanged() {
		onBackPressedCallback?.onHistoryChanged()
	}

	override fun onTitleChanged(title: CharSequence, subtitle: CharSequence?) {
		setTitle(title)
		supportActionBar?.subtitle = subtitle?.toString()?.toHttpUrlOrNull()?.topPrivateDomain() ?: subtitle
	}

	class Contract : ActivityResultContract<Pair<String, Headers?>, TaggedActivityResult>() {
		override fun createIntent(context: Context, input: Pair<String, Headers?>): Intent {
			return newIntent(context, input.first, input.second)
		}

		override fun parseResult(resultCode: Int, intent: Intent?): TaggedActivityResult {
			return TaggedActivityResult(TAG, resultCode)
		}
	}

	companion object {

		const val TAG = "CloudFlareActivity"
		private const val ARG_UA = "ua"

		fun newIntent(
			context: Context,
			url: String,
			headers: Headers?,
		) = Intent(context, CloudFlareActivity::class.java).apply {
			data = url.toUri()
			headers?.get(CommonHeaders.USER_AGENT)?.let {
				putExtra(ARG_UA, it)
			}
		}
	}
}
