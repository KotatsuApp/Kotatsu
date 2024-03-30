package org.koitharu.kotatsu.core.exceptions.resolve

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import okhttp3.Headers
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.ui.AlternativesActivity
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareActivity
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.core.ui.dialog.ErrorDetailsDialog
import org.koitharu.kotatsu.core.util.TaggedActivityResult
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExceptionResolver : ActivityResultCallback<TaggedActivityResult> {

	private val continuations = ArrayMap<String, Continuation<Boolean>>(1)
	private val activity: FragmentActivity?
	private val fragment: Fragment?
	private val sourceAuthContract: ActivityResultLauncher<MangaSource>
	private val cloudflareContract: ActivityResultLauncher<Pair<String, Headers?>>

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
		is CloudFlareProtectedException -> resolveCF(e.url, e.headers)
		is AuthRequiredException -> resolveAuthException(e.source)
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

	private suspend fun resolveCF(url: String, headers: Headers): Boolean = suspendCoroutine { cont ->
		continuations[CloudFlareActivity.TAG] = cont
		cloudflareContract.launch(url to headers)
	}

	private suspend fun resolveAuthException(source: MangaSource): Boolean = suspendCoroutine { cont ->
		continuations[SourceAuthActivity.TAG] = cont
		sourceAuthContract.launch(source)
	}

	private fun openInBrowser(url: String) {
		val context = activity ?: fragment?.activity ?: return
		context.startActivity(BrowserActivity.newIntent(context, url, null, null))
	}

	private fun openAlternatives(manga: Manga) {
		val context = activity ?: fragment?.activity ?: return
		context.startActivity(AlternativesActivity.newIntent(context, manga))
	}

	private fun getFragmentManager() = checkNotNull(fragment?.childFragmentManager ?: activity?.supportFragmentManager)

	companion object {

		@StringRes
		fun getResolveStringId(e: Throwable) = when (e) {
			is CloudFlareProtectedException -> R.string.captcha_solve
			is AuthRequiredException -> R.string.sign_in
			is NotFoundException -> if (e.url.isNotEmpty()) R.string.open_in_browser else 0
			is UnsupportedSourceException -> if (e.manga != null) R.string.alternatives else 0
			else -> 0
		}

		fun canResolve(e: Throwable) = getResolveStringId(e) != 0
	}
}
