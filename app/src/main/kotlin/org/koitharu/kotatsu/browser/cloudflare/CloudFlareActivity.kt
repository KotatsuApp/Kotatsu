package org.koitharu.kotatsu.browser.cloudflare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.Insets
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.yield
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.WebViewBackPressedCallback
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.network.cookies.MutableCookieJar
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.configureForParser
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding
import org.koitharu.kotatsu.parsers.network.CloudFlareHelper
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
		val url = intent?.dataString
		if (url.isNullOrEmpty()) {
			finishAfterTransition()
			return
		}
		cfClient = CloudFlareClient(cookieJar, this, url)
		viewBinding.webView.configureForParser(intent?.getStringExtra(AppRouter.KEY_USER_AGENT))
		viewBinding.webView.webViewClient = cfClient
		onBackPressedCallback = WebViewBackPressedCallback(viewBinding.webView).also {
			onBackPressedDispatcher.addCallback(it)
		}
		if (savedInstanceState == null) {
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
		val source = intent?.getStringExtra(AppRouter.KEY_SOURCE)
		if (source != null) {
			CaptchaNotifier(this).dismiss(MangaSource(source))
		}
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
			CloudFlareHelper.isCloudFlareCookie(cookie.name)
		}
	}

	class Contract : ActivityResultContract<CloudFlareProtectedException, Boolean>() {
		override fun createIntent(context: Context, input: CloudFlareProtectedException): Intent {
			return AppRouter.cloudFlareResolveIntent(context, input)
		}

		override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
			return resultCode == RESULT_OK
		}
	}

	companion object {

		const val TAG = "CloudFlareActivity"
	}
}
