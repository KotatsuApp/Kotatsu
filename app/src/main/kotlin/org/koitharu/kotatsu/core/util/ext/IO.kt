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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

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

fun InputStream.toByteBuffer(): ByteBuffer {
	val outStream = ByteArrayOutputStream(available())
	copyTo(outStream)
	val bytes = outStream.toByteArray()
	return ByteBuffer.allocateDirect(bytes.size).put(bytes).position(0) as ByteBuffer
}
