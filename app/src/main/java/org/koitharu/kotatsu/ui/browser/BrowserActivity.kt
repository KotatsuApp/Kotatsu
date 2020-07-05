package org.koitharu.kotatsu.ui.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_browser.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity

@SuppressLint("SetJavaScriptEnabled")
class BrowserActivity : BaseActivity(), BrowserCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_browser)
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(R.drawable.ic_cross)
		}
		with(webView.settings) {
			javaScriptEnabled = true
		}
		webView.webViewClient = BrowserClient(this)
		val url = intent?.dataString
		if (url.isNullOrEmpty()) {
			finishAfterTransition()
		} else {
			webView.loadUrl(url)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_browser, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			webView.stopLoading()
			finishAfterTransition()
			true
		}
		R.id.action_browser -> {
			val intent = Intent(Intent.ACTION_VIEW)
			intent.data = Uri.parse(webView.url)
			try {
				startActivity(Intent.createChooser(intent, item.title))
			} catch (_: ActivityNotFoundException) {
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack()
		} else {
			super.onBackPressed()
		}
	}

	override fun onPause() {
		webView.onPause()
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
		webView.onResume()
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onTitleChanged(title: CharSequence, subtitle: CharSequence?) {
		this.title = title
		supportActionBar?.subtitle = subtitle
	}

	companion object {

		@JvmStatic
		fun newIntent(context: Context, url: String) = Intent(context, BrowserActivity::class.java)
			.setData(Uri.parse(url))
	}
}