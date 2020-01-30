package org.koitharu.kotatsu.utils.ext

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await() = suspendCancellableCoroutine<Response> { cont ->
	this.enqueue(object : Callback {
		override fun onFailure(call: Call, e: IOException) {
			if (!cont.isCancelled) {
				cont.resumeWithException(e)
			}
		}

		override fun onResponse(call: Call, response: Response) {
			cont.resume(response)
		}
	})
	cont.invokeOnCancellation {
		safe {
			this.cancel()
		}
	}
}