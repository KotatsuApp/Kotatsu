package org.koitharu.kotatsu.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.R as materialR
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.network.UserAgentInterceptor
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding

@SuppressLint("SetJavaScriptEnabled")
class BrowserActivity : BaseActivity<ActivityBrowserBinding>(), BrowserCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityBrowserBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		with(binding.webView.settings) {
			javaScriptEnabled = true
			userAgentString = UserAgentInterceptor.userAgentChrome
		}
		binding.webView.webViewClient = BrowserClient(this)
		binding.webView.webChromeClient = ProgressChromeClient(binding.progressBar)
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
			binding.webView.loadUrl(url)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		binding.webView.saveState(outState)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		binding.webView.restoreState(savedInstanceState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_browser, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			binding.webView.stopLoading()
			finishAfterTransition()
			true
		}
		R.id.action_browser -> {
			val intent = Intent(Intent.ACTION_VIEW)
			intent.data = Uri.parse(binding.webView.url)
			try {
				startActivity(Intent.createChooser(intent, item.title))
			} catch (_: ActivityNotFoundException) {
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onBackPressed() {
		if (binding.webView.canGoBack()) {
			binding.webView.goBack()
		} else {
			super.onBackPressed()
		}
	}

	override fun onPause() {
		binding.webView.onPause()
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
		binding.webView.onResume()
	}

	override fun onDestroy() {
		super.onDestroy()
		binding.webView.destroy()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading
	}

	override fun onTitleChanged(title: CharSequence, subtitle: CharSequence?) {
		this.title = title
		supportActionBar?.subtitle = subtitle
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.appbar.updatePadding(
			top = insets.top,
		)
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
			bottom = insets.bottom,
		)
	}

	companion object {

		private const val EXTRA_TITLE = "title"

		fun newIntent(context: Context, url: String, title: String?): Intent {
			return Intent(context, BrowserActivity::class.java)
				.setData(Uri.parse(url))
				.putExtra(EXTRA_TITLE, title)
		}
	}
}
