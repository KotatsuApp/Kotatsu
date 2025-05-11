package org.koitharu.kotatsu.core.exceptions.resolve

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.StringRes
import androidx.collection.MutableScatterMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareActivity
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.InteractiveActionRequiredException
import org.koitharu.kotatsu.core.exceptions.ProxyConfigException
import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.core.util.ext.isHttpUrl
import org.koitharu.kotatsu.core.util.ext.restartApplication
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.scrobbling.common.domain.ScrobblerAuthRequiredException
import org.koitharu.kotatsu.scrobbling.common.ui.ScrobblerAuthHelper
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import java.security.cert.CertPathValidatorException
import javax.inject.Provider
import javax.net.ssl.SSLException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExceptionResolver @AssistedInject constructor(
	@Assisted private val host: Host,
	private val settings: AppSettings,
	private val scrobblerAuthHelperProvider: Provider<ScrobblerAuthHelper>,
) {
	private val continuations = MutableScatterMap<String, Continuation<Boolean>>(1)

	private val browserActionContract = host.registerForActivityResult(BrowserActivity.Contract()) {
		handleActivityResult(BrowserActivity.TAG, true)
	}
	private val sourceAuthContract = host.registerForActivityResult(SourceAuthActivity.Contract()) {
		handleActivityResult(SourceAuthActivity.TAG, it)
	}
	private val cloudflareContract = host.registerForActivityResult(CloudFlareActivity.Contract()) {
		handleActivityResult(CloudFlareActivity.TAG, it)
	}

	fun showErrorDetails(e: Throwable, url: String? = null) {
		host.router()?.showErrorDialog(e, url)
	}

	suspend fun resolve(e: Throwable): Boolean = when (e) {
		is CloudFlareProtectedException -> resolveCF(e)
		is AuthRequiredException -> resolveAuthException(e.source)
		is SSLException,
		is CertPathValidatorException -> {
			showSslErrorDialog()
			false
		}

		is InteractiveActionRequiredException -> resolveBrowserAction(e)

		is ProxyConfigException -> {
			host.router()?.openProxySettings()
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

		is ScrobblerAuthRequiredException -> {
			val authHelper = scrobblerAuthHelperProvider.get()
			if (authHelper.isAuthorized(e.scrobbler)) {
				true
			} else {
				host.withContext {
					authHelper.startAuth(this, e.scrobbler).onFailure(::showErrorDetails)
				}
				false
			}
		}

		else -> false
	}

	private suspend fun resolveBrowserAction(
		e: InteractiveActionRequiredException
	): Boolean = suspendCoroutine { cont ->
		continuations[BrowserActivity.TAG] = cont
		browserActionContract.launch(e)
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
		host.router()?.openBrowser(url, null, null)
	}

	private fun openAlternatives(manga: Manga) {
		host.router()?.openAlternatives(manga)
	}

	private fun handleActivityResult(tag: String, result: Boolean) {
		continuations.remove(tag)?.resume(result)
	}

	private fun showSslErrorDialog() {
		val ctx = host.getContext() ?: return
		if (settings.isSSLBypassEnabled) {
			Toast.makeText(ctx, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
			return
		}
		buildAlertDialog(ctx) {
			setTitle(R.string.ignore_ssl_errors)
			setMessage(R.string.ignore_ssl_errors_summary)
			setPositiveButton(R.string.apply) { _, _ ->
				settings.isSSLBypassEnabled = true
				Toast.makeText(ctx, R.string.settings_apply_restart_required, Toast.LENGTH_LONG).show()
				ctx.restartApplication()
			}
			setNegativeButton(android.R.string.cancel, null)
		}.show()
	}

	private inline fun Host.withContext(block: Context.() -> Unit) {
		getContext()?.apply(block)
	}

	private fun Host.router(): AppRouter? = when (this) {
		is FragmentActivity -> router
		is Fragment -> router
		else -> null
	}

	interface Host : ActivityResultCaller {

		fun getChildFragmentManager(): FragmentManager

		fun getContext(): Context?
	}

	@AssistedFactory
	interface Factory {

		fun create(host: Host): ExceptionResolver
	}

	companion object {

		@StringRes
		fun getResolveStringId(e: Throwable) = when (e) {
			is CloudFlareProtectedException -> R.string.captcha_solve
			is ScrobblerAuthRequiredException,
			is AuthRequiredException -> R.string.sign_in

			is NotFoundException -> if (e.url.isHttpUrl()) R.string.open_in_browser else 0
			is UnsupportedSourceException -> if (e.manga != null) R.string.alternatives else 0
			is SSLException,
			is CertPathValidatorException -> R.string.fix

			is ProxyConfigException -> R.string.settings

			is InteractiveActionRequiredException -> R.string._continue

			else -> 0
		}

		fun canResolve(e: Throwable) = getResolveStringId(e) != 0
	}
}
