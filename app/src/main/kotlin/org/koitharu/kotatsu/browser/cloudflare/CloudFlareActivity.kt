package org.koitharu.kotatsu.browser.cloudflare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.yield
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.WebViewBackPressedCallback
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.TaggedActivityResult
import org.koitharu.kotatsu.core.util.ext.configureForParser
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class CloudFlareActivity : BaseActivity<ActivityBrowserBinding>(), CloudFlareCallback {

	private var pendingResult = RESULT_CANCELED

	@Inject
	lateinit var cookieJar: MutableCookieJar

	private lateinit var cfClient: CloudFlareClient
	private var onBackPressedCallback: WebViewBackPressedCallback? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (!setContentViewWebViewSafe { ActivityBrowserBinding.inflate(layoutInflater) }) {
			return
		}
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		val url = intent?.dataString.orEmpty()
		cfClient = CloudFlareClient(cookieJar, this, url)
		viewBinding.webView.configureForParser(intent?.getStringExtra(ARG_UA))
		viewBinding.webView.webViewClient = cfClient
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
		runCatching {
			viewBinding.webView
		}.onSuccess {
			it.stopLoading()
			it.destroy()
		}
		super.onDestroy()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_captcha, menu)
		return super.onCreateOptionsMenu(menu)
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

		R.id.action_retry -> {
			restartCheck()
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

	override fun onLoopDetected() {
		restartCheck()
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
		supportActionBar?.subtitle =
			subtitle?.toString()?.toHttpUrlOrNull()?.topPrivateDomain() ?: subtitle
	}

	private fun restartCheck() {
		lifecycleScope.launch {
			viewBinding.webView.stopLoading()
			yield()
			cfClient.reset()
			val targetUrl = intent?.dataString?.toHttpUrlOrNull()
			if (targetUrl != null) {
				clearCfCookies(targetUrl)
				viewBinding.webView.loadUrl(targetUrl.toString())
			}
		}
	}

	private suspend fun clearCfCookies(url: HttpUrl) = runInterruptible(Dispatchers.Default) {
		cookieJar.removeCookies(url) { cookie ->
			val name = cookie.name
			name.startsWith("cf_") || name.startsWith("_cf") || name.startsWith("__cf") || name == "csrftoken"
		}
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
