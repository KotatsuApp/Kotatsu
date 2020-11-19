package org.koitharu.kotatsu.utils.ext

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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

fun CoroutineScope.launchAfter(
	job: Job?,
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> Unit
): Job = launch(context, start) {
	try {
		job?.join()
	} catch (e: Throwable) {
		if (BuildConfig.DEBUG) {
			e.printStackTrace()
		}
	}
	block()
}

fun CoroutineScope.launchInstead(
	job: Job?,
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> Unit
): Job = launch(context, start) {
	try {
		job?.cancelAndJoin()
	} catch (e: Throwable) {
		if (BuildConfig.DEBUG) {
			e.printStackTrace()
		}
	}
	block()
}

val IgnoreErrors
	get() = CoroutineExceptionHandler { _, e ->
		if (BuildConfig.DEBUG) {
			e.printStackTrace()
		}
	}

val processLifecycleScope: CoroutineScope
	inline get() = ProcessLifecycleOwner.get().lifecycleScope