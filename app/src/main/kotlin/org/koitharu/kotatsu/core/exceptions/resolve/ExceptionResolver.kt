package org.koitharu.kotatsu.core.exceptions.resolve

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.collection.MutableScatterMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.EntryPointAccessors
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.ui.AlternativesActivity
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareActivity
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.ProxyConfigException
import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseActivity.BaseActivityEntryPoint
import org.koitharu.kotatsu.core.ui.dialog.ErrorDetailsDialog
import org.koitharu.kotatsu.core.util.TaggedActivityResult
import org.koitharu.kotatsu.core.util.ext.findActivity
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExceptionResolver : ActivityResultCallback<TaggedActivityResult> {

	private val continuations = MutableScatterMap<String, Continuation<Boolean>>(1)
	private val activity: FragmentActivity?
	private val fragment: Fragment?
	private val sourceAuthContract: ActivityResultLauncher<MangaSource>
	private val cloudflareContract: ActivityResultLauncher<CloudFlareProtectedException>

	val context: Context?
		get() = activity ?: fragment?.context

	constructor(activity: FragmentActivity) {
		this.activity = activity
		fragment = null
		sourceAuthContract = activity.registerForActivityResult(SourceAuthActivity.Contract(), this)
		cloudflareContract = activity.registerForActivityResult(CloudFlareActivity.Contract(), this)
	}

	constructor(fragment: Fragment) {
		this.fragment = fragment
		activity = null
		sourceAuthContract = fragment.registerForActivityResult(SourceAuthActivity.Contract(), this)
		cloudflareContract = fragment.registerForActivityResult(CloudFlareActivity.Contract(), this)
	}

	override fun onActivityResult(result: TaggedActivityResult) {
		continuations.remove(result.tag)?.resume(result.isSuccess)
	}

	fun showDetails(e: Throwable, url: String?) {
		ErrorDetailsDialog.show(getFragmentManager(), e, url)
	}

	suspend fun resolve(e: Throwable): Boolean = when (e) {
		is CloudFlareProtectedException -> resolveCF(e)
		is AuthRequiredException -> resolveAuthException(e.source)
		is SSLException,
		is CertPathValidatorException -> {
			showSslErrorDialog()
			false
		}

		is ProxyConfigException -> {
			context?.run {
				startActivity(SettingsActivity.newProxySettingsIntent(this))
			}
			false
		}

		is NotFoundException -> {
			openInBrowser(e.url)
			false
		}

		is UnsupportedSourceException -> {
			e.manga?.let { openAlternatives(it) }
			false
		}

		else -> false
	}

	private suspend fun resolveCF(e: CloudFlareProtectedException): Boolean = suspendCoroutine { cont ->
		continuations[CloudFlareActivity.TAG] = cont
		cloudflareContract.launch(e)
	}

	private suspend fun resolveAuthException(source: MangaSource): Boolean = suspendCoroutine { cont ->
		continuations[SourceAuthActivity.TAG] = cont
		sourceAuthContract.launch(source)
	}

	private fun openInBrowser(url: String) {
		context?.run {
			startActivity(BrowserActivity.newIntent(this, url, null, null))
		}
	}

	private fun openAlternatives(manga: Manga) {
		context?.run {
			startActivity(AlternativesActivity.newIntent(this, manga))
		}
	}

	private fun showSslErrorDialog() {
		val ctx = context ?: return
		val settings = getAppSettings(ctx)
		if (settings.isSSLBypassEnabled) {
			Toast.makeText(ctx, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
			return
		}
		MaterialAlertDialogBuilder(ctx)
			.setTitle(R.string.ignore_ssl_errors)
			.setMessage(R.string.ignore_ssl_errors_summary)
			.setPositiveButton(R.string.apply) { _, _ ->
				settings.isSSLBypassEnabled = true
				Toast.makeText(ctx, R.string.settings_apply_restart_required, Toast.LENGTH_SHORT).show()
				ctx.findActivity()?.finishAffinity()
			}.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	private fun getAppSettings(context: Context): AppSettings {
		return EntryPointAccessors.fromApplication<BaseActivityEntryPoint>(context).settings
	}

	private fun getFragmentManager() = checkNotNull(fragment?.childFragmentManager ?: activity?.supportFragmentManager)

	companion object {

		@StringRes
		fun getResolveStringId(e: Throwable) = when (e) {
			is CloudFlareProtectedException -> R.string.captcha_solve
			is AuthRequiredException -> R.string.sign_in
			is NotFoundException -> if (e.url.isNotEmpty()) R.string.open_in_browser else 0
			is UnsupportedSourceException -> if (e.manga != null) R.string.alternatives else 0
			is SSLException,
			is CertPathValidatorException -> R.string.fix

			is ProxyConfigException -> R.string.settings

			else -> 0
		}

		fun canResolve(e: Throwable) = getResolveStringId(e) != 0
	}
}
