package org.koitharu.kotatsu.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.userAgent
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.ParserMangaRepository
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.configureForParser
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class BrowserActivity : BaseActivity<ActivityBrowserBinding>(), BrowserCallback {

	private lateinit var onBackPressedCallback: WebViewBackPressedCallback

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (!setContentViewWebViewSafe { ActivityBrowserBinding.inflate(layoutInflater) }) {
			return
		}
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		val mangaSource = MangaSource(intent?.getStringExtra(EXTRA_SOURCE))
		val repository = mangaRepositoryFactory.create(mangaSource) as? ParserMangaRepository
		repository?.getRequestHeaders()?.get(CommonHeaders.USER_AGENT)
		viewBinding.webView.configureForParser(userAgent)
		CookieManager.getInstance().setAcceptThirdPartyCookies(viewBinding.webView, true)
		viewBinding.webView.webViewClient = BrowserClient(this)
		viewBinding.webView.webChromeClient = ProgressChromeClient(viewBinding.progressBar)
		onBackPressedCallback = WebViewBackPressedCallback(viewBinding.webView)
		onBackPressedDispatcher.addCallback(onBackPressedCallback)
		if (savedInstanceState != null) {
			return
		}
		val url = intent?.dataString
		if (url.isNullOrEmpty()) {
			finishAfterTransition()
		} else {
			onTitleChanged(
				intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.loading_),
				url,
			)
			viewBinding.webView.loadUrl(url)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_browser, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			viewBinding.webView.stopLoading()
			finishAfterTransition()
			true
		}

		R.id.action_browser -> {
			val url = viewBinding.webView.url?.toUriOrNull()
			if (url != null) {
				val intent = Intent(Intent.ACTION_VIEW)
				intent.data = url
				try {
					startActivity(Intent.createChooser(intent, item.title))
				} catch (_: ActivityNotFoundException) {
				}
			}
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

	override fun onDestroy() {
		super.onDestroy()
		if (hasViewBinding()) {
			viewBinding.webView.stopLoading()
			viewBinding.webView.destroy()
		}
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.progressBar.isVisible = isLoading
	}

	override fun onTitleChanged(title: CharSequence, subtitle: CharSequence?) {
		this.title = title
		supportActionBar?.subtitle = subtitle
	}

	override fun onHistoryChanged() {
		onBackPressedCallback.onHistoryChanged()
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

	companion object {

		private const val EXTRA_TITLE = "title"
		private const val EXTRA_SOURCE = "source"

		fun newIntent(context: Context, url: String, source: MangaSource?, title: String?): Intent {
			return Intent(context, BrowserActivity::class.java)
				.setData(Uri.parse(url))
				.putExtra(EXTRA_TITLE, title)
				.putExtra(EXTRA_SOURCE, source?.name)
		}
	}
}
