package org.koitharu.kotatsu.core.exceptions.resolve

import android.util.ArrayMap
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import org.koitharu.kotatsu.utils.TaggedActivityResult
import org.koitharu.kotatsu.utils.isSuccess
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExceptionResolver private constructor(
	private val activity: FragmentActivity?,
	private val fragment: Fragment?,
) : ActivityResultCallback<TaggedActivityResult> {

	private val continuations = ArrayMap<String, Continuation<Boolean>>(1)
	private lateinit var sourceAuthContract: ActivityResultLauncher<MangaSource>

	constructor(activity: FragmentActivity) : this(activity = activity, fragment = null) {
		sourceAuthContract = activity.registerForActivityResult(SourceAuthActivity.Contract(), this)
	}

	constructor(fragment: Fragment) : this(activity = null, fragment = fragment) {
		sourceAuthContract = fragment.registerForActivityResult(SourceAuthActivity.Contract(), this)
	}

	override fun onActivityResult(result: TaggedActivityResult?) {
		result ?: return
		continuations.remove(result.tag)?.resume(result.isSuccess)
	}

	suspend fun resolve(e: Throwable): Boolean = when (e) {
		is CloudFlareProtectedException -> resolveCF(e.url)
		is AuthRequiredException -> resolveAuthException(e.source)
		else -> false
	}

	private suspend fun resolveCF(url: String): Boolean {
		val dialog = CloudFlareDialog.newInstance(url)
		val fm = getFragmentManager()
		return suspendCancellableCoroutine { cont ->
			fm.clearFragmentResult(CloudFlareDialog.TAG)
			continuations[CloudFlareDialog.TAG] = cont
			fm.setFragmentResultListener(CloudFlareDialog.TAG, checkNotNull(fragment ?: activity)) { key, result ->
				continuations.remove(key)?.resume(result.getBoolean(CloudFlareDialog.EXTRA_RESULT))
			}
			dialog.show(fm, CloudFlareDialog.TAG)
			cont.invokeOnCancellation {
				continuations.remove(CloudFlareDialog.TAG, cont)
				fm.clearFragmentResultListener(CloudFlareDialog.TAG)
				dialog.dismiss()
			}
		}
	}

	private suspend fun resolveAuthException(source: MangaSource): Boolean = suspendCoroutine { cont ->
		continuations[SourceAuthActivity.TAG] = cont
		sourceAuthContract.launch(source)
	}

	private fun getFragmentManager() = checkNotNull(fragment?.childFragmentManager ?: activity?.supportFragmentManager)

	companion object {

		@StringRes
		fun getResolveStringId(e: Throwable) = when (e) {
			is CloudFlareProtectedException -> R.string.captcha_solve
			is AuthRequiredException -> R.string.sign_in
			else -> 0
		}

		fun canResolve(e: Throwable) = getResolveStringId(e) != 0
	}
}