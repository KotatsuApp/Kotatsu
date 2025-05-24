package org.koitharu.kotatsu.core.util.ext

import android.content.BroadcastReceiver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.util.AcraCoroutineErrorHandler
import org.koitharu.kotatsu.core.util.RetainedLifecycleCoroutineScope
import org.koitharu.kotatsu.parsers.util.cancelAll
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

val processLifecycleScope: CoroutineScope
	get() = ProcessLifecycleOwner.get().lifecycleScope + AcraCoroutineErrorHandler()

val RetainedLifecycle.lifecycleScope: RetainedLifecycleCoroutineScope
	inline get() = RetainedLifecycleCoroutineScope(this)

fun <T> Deferred<T>.getCompletionResultOrNull(): Result<T>? = if (isCompleted) {
	getCompletionExceptionOrNull()?.let { error ->
		Result.failure(error)
	} ?: Result.success(getCompleted())
} else {
	null
}

fun <T> Deferred<T>.peek(): T? = if (isCompleted) {
	runCatchingCancellable {
		getCompleted()
	}.getOrNull()
} else {
	null
}

@Suppress("SuspendFunctionOnCoroutineScope")
suspend fun CoroutineScope.cancelChildrenAndJoin(cause: CancellationException? = null) {
	val jobs = coroutineContext[Job]?.children?.toList() ?: return
	jobs.cancelAll(cause)
	jobs.joinAll()
}

fun BroadcastReceiver.goAsync(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) {
	val pendingResult = goAsync()
	processLifecycleScope.launch(context) {
		try {
			block()
		} finally {
			pendingResult.finish()
		}
	}
}
