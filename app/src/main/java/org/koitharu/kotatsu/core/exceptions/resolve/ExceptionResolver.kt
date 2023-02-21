package org.koitharu.kotatsu.core.exceptions.resolve

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Headers
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.settings.sources.auth.SourceAuthActivity
import org.koitharu.kotatsu.utils.TaggedActivityResult
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
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
		is CloudFlareProtectedException -> resolveCF(e.url, e.headers)
		is AuthRequiredException -> resolveAuthException(e.source)
		is NotFoundException -> {
			openInBrowser(e.url)
			false
		}

		else -> false
	}

	private suspend fun resolveCF(url: String, headers: Headers): Boolean {
		val dialog = CloudFlareDialog.newInstance(url, headers)
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
				dialog.dismissAllowingStateLoss()
			}
		}
	}

	private suspend fun resolveAuthException(source: MangaSource): Boolean = suspendCoroutine { cont ->
		continuations[SourceAuthActivity.TAG] = cont
		sourceAuthContract.launch(source)
	}

	private fun openInBrowser(url: String) {
		val context = activity ?: fragment?.activity ?: return
		context.startActivity(BrowserActivity.newIntent(context, url, null))
	}

	private fun getFragmentManager() = checkNotNull(fragment?.childFragmentManager ?: activity?.supportFragmentManager)

	companion object {

		@StringRes
		fun getResolveStringId(e: Throwable) = when (e) {
			is CloudFlareProtectedException -> R.string.captcha_solve
			is AuthRequiredException -> R.string.sign_in
			is NotFoundException -> if (e.url.isNotEmpty()) R.string.open_in_browser else 0
			else -> 0
		}

		fun canResolve(e: Throwable) = getResolveStringId(e) != 0

		fun showDetails(context: Context, e: Throwable) {
			val stackTrace = e.stackTraceToString()
			val dialog = MaterialAlertDialogBuilder(context)
				.setTitle(e.getDisplayMessage(context.resources))
				.setMessage(stackTrace)
				.setPositiveButton(androidx.preference.R.string.copy) { _, _ ->
					val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
					clipboardManager.setPrimaryClip(
						ClipData.newPlainText(context.getString(R.string.error), stackTrace),
					)
				}
				.setNegativeButton(R.string.close, null)
				.create()
			dialog.show()
		}
	}
}
