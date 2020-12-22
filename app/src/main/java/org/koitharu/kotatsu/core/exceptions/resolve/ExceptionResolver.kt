package org.koitharu.kotatsu.core.exceptions.resolve

import android.util.ArrayMap
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ExceptionResolver(
	private val lifecycleOwner: LifecycleOwner,
	private val fm: FragmentManager
) {

	private val continuations = ArrayMap<String, Continuation<Boolean>>(1)

	suspend fun resolve(e: ResolvableException): Boolean = when (e) {
		is CloudFlareProtectedException -> resolveCF(e.url)
		else -> false
	}

	private suspend fun resolveCF(url: String) = suspendCancellableCoroutine<Boolean> { cont ->
		val dialog = CloudFlareDialog.newInstance(url)
		fm.clearFragmentResult(CloudFlareDialog.TAG)
		continuations[CloudFlareDialog.TAG] = cont
		fm.setFragmentResultListener(CloudFlareDialog.TAG, lifecycleOwner) { key, result ->
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