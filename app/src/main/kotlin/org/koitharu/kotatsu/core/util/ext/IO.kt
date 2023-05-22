package org.koitharu.kotatsu.core.util.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.Source
import org.koitharu.kotatsu.core.util.CancellableSource
import org.koitharu.kotatsu.core.util.progress.ProgressResponseBody

fun ResponseBody.withProgress(progressState: MutableStateFlow<Float>): ResponseBody {
	return ProgressResponseBody(this, progressState)
}

suspend fun Source.cancellable(): Source {
	val job = currentCoroutineContext()[Job]
	return CancellableSource(job, this)
}

suspend fun BufferedSink.writeAllCancellable(source: Source) = withContext(Dispatchers.IO) {
	writeAll(source.cancellable())
}
