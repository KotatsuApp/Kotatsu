package org.koitharu.kotatsu.settings.sources.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.BrowserCallback
import org.koitharu.kotatsu.browser.BrowserClient
import org.koitharu.kotatsu.browser.ProgressChromeClient
import org.koitharu.kotatsu.browser.WebViewBackPressedCallback
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.ParserMangaRepository
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.TaggedActivityResult
import org.koitharu.kotatsu.core.util.ext.configureForParser
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class SourceAuthActivity : BaseActivity<ActivityBrowserBinding>(), BrowserCallback {

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	private lateinit var onBackPressedCallback: WebViewBackPressedCallback
	private lateinit var authProvider: MangaParserAuthProvider

	@SuppressLint("SetJavaScriptEnabled")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (!setContentViewWebViewSafe { ActivityBrowserBinding.inflate(layoutInflater) }) {
			return
		}
		val source = MangaSource(intent?.getStringExtra(EXTRA_SOURCE))
		if (source !is MangaParserSource) {
			finishAfterTransition()
			return
		}
		val repository = mangaRepositoryFactory.create(source) as? ParserMangaRepository
		authProvider = (repository)?.getAuthProvider() ?: run {
			Toast.makeText(
				this,
				getString(R.string.auth_not_supported_by, source.title),
				Toast.LENGTH_SHORT,
			).show()
			finishAfterTransition()
			return
		}
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		viewBinding.webView.configureForParser(repository.getRequestHeaders()[CommonHeaders.USER_AGENT])
		CookieManager.getInstance().setAcceptThirdPartyCookies(viewBinding.webView, true)
		viewBinding.webView.webViewClient = BrowserClient(this)
		viewBinding.webView.webChromeClient = ProgressChromeClient(viewBinding.progressBar)
		onBackPressedCallback = WebViewBackPressedCallback(viewBinding.webView)
		onBackPressedDispatcher.addCallback(onBackPressedCallback)
		if (savedInstanceState != null) {
			return
		}
		val url = authProvider.authUrl
		onTitleChanged(
			source.title,
			getString(R.string.loading_),
		)
		viewBinding.webView.loadUrl(url)
	}

	override fun onDestroy() {
		super.onDestroy()
		viewBinding.webView.destroy()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			viewBinding.webView.stopLoading()
			setResult(Activity.RESULT_CANCELED)
			finishAfterTransition()
			true
		}

		else -> super.onOptionsItemSelected(item)
	}

	override fun onPause() {
		viewBinding.webView.onPause()
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
		viewBinding.webView.onResume()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.progressBar.isVisible = isLoading
		if (!isLoading && authProvider.isAuthorized) {
			Toast.makeText(this, R.string.auth_complete, Toast.LENGTH_SHORT).show()
			setResult(Activity.RESULT_OK)
			finishAfterTransition()
		}
	}

	override fun onTitleChanged(title: CharSequence, subtitle: CharSequence?) {
		this.title = title
		supportActionBar?.subtitle = subtitle
	}

	override fun onHistoryChanged() {
		onBackPressedCallback.onHistoryChanged()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.appbar.updatePadding(top = insets.top)
		viewBinding.webView.updatePadding(bottom = insets.bottom)
	}

	class Contract : ActivityResultContract<MangaSource, TaggedActivityResult>() {
		override fun createIntent(context: Context, input: MangaSource): Intent {
			return newIntent(context, input)
		}

		override fun parseResult(resultCode: Int, intent: Intent?): TaggedActivityResult {
			return TaggedActivityResult(TAG, resultCode)
		}
	}

	companion object {

		private const val EXTRA_SOURCE = "source"
		const val TAG = "SourceAuthActivity"

		fun newIntent(context: Context, source: MangaSource): Intent {
			return Intent(context, SourceAuthActivity::class.java)
				.putExtra(EXTRA_SOURCE, source.name)
		}
	}
}
