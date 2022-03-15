package org.koitharu.kotatsu.settings.sources.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.browser.BrowserCallback
import org.koitharu.kotatsu.browser.BrowserClient
import org.koitharu.kotatsu.browser.ProgressChromeClient
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.databinding.ActivityBrowserBinding
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.TaggedActivityResult
import com.google.android.material.R as materialR

class SourceAuthActivity : BaseActivity<ActivityBrowserBinding>(), BrowserCallback {

	private lateinit var authProvider: MangaParserAuthProvider

	@SuppressLint("SetJavaScriptEnabled")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityBrowserBinding.inflate(layoutInflater))
		val source = intent?.getSerializableExtra(EXTRA_SOURCE) as? MangaSource
		if (source == null) {
			finishAfterTransition()
			return
		}
		authProvider = (MangaRepository(source) as? RemoteMangaRepository)?.getAuthProvider() ?: run {
			Toast.makeText(
				this,
				getString(R.string.auth_not_supported_by, source.title),
				Toast.LENGTH_SHORT
			).show()
			finishAfterTransition()
			return
		}
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(materialR.drawable.abc_ic_clear_material)
		}
		with(binding.webView.settings) {
			javaScriptEnabled = true
		}
		binding.webView.webViewClient = BrowserClient(this)
		binding.webView.webChromeClient = ProgressChromeClient(binding.progressBar)
		if (savedInstanceState != null) {
			return
		}
		val url = authProvider.authUrl
		onTitleChanged(
			source.title,
			getString(R.string.loading_)
		)
		binding.webView.loadUrl(url)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		binding.webView.saveState(outState)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		binding.webView.restoreState(savedInstanceState)
	}

	override fun onDestroy() {
		super.onDestroy()
		binding.webView.destroy()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			binding.webView.stopLoading()
			setResult(Activity.RESULT_CANCELED)
			finishAfterTransition()
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

	override fun onLoadingStateChanged(isLoading: Boolean) {
		binding.progressBar.isVisible = isLoading
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

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.appbar.updatePadding(top = insets.top)
		binding.webView.updatePadding(bottom = insets.bottom)
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
				.putExtra(EXTRA_SOURCE, source as Parcelable)
		}
	}
}