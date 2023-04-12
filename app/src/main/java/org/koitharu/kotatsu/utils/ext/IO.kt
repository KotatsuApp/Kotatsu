package org.koitharu.kotatsu.utils.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.koitharu.kotatsu.utils.progress.ProgressResponseBody
import java.io.InputStream
import java.io.OutputStream

suspend fun InputStream.copyToSuspending(
	out: OutputStream,
	bufferSize: Int = DEFAULT_BUFFER_SIZE,
	progressState: MutableStateFlow<Float>? = null,
): Long = withContext(Dispatchers.IO) {
	val job = currentCoroutineContext()[Job]
	val total = available()
	var bytesCopied: Long = 0
	val buffer = ByteArray(bufferSize)
	var bytes = read(buffer)
	while (bytes >= 0) {
		out.write(buffer, 0, bytes)
		bytesCopied += bytes
		job?.ensureActive()
		bytes = read(buffer)
		job?.ensureActive()
		if (progressState != null && total > 0) {
			progressState.value = bytesCopied / total.toFloat()
		}
	}
	bytesCopied
}

fun ResponseBody.withProgress(progressState: MutableStateFlow<Float>): ResponseBody {
	return ProgressResponseBody(this, progressState)
}
