package org.koitharu.kotatsu.core.util.progress

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

class ProgressResponseBody(
	private val delegate: ResponseBody,
	private val progressState: MutableStateFlow<Float>,
) : ResponseBody() {

	private var bufferedSource: BufferedSource? = null

	override fun close() {
		super.close()
		delegate.close()
	}

	override fun contentLength(): Long = delegate.contentLength()

	override fun contentType(): MediaType? = delegate.contentType()

	override fun source(): BufferedSource {
		return bufferedSource ?: synchronized(this) {
			bufferedSource ?: ProgressSource(delegate.source(), contentLength(), progressState).buffer().also {
				bufferedSource = it
			}
		}
	}

	private class ProgressSource(
		delegate: Source,
		private val contentLength: Long,
		private val progressState: MutableStateFlow<Float>,
	) : ForwardingSource(delegate) {

		private var totalBytesRead = 0L

		override fun read(sink: Buffer, byteCount: Long): Long {
			val bytesRead = super.read(sink, byteCount)
			if (contentLength > 0) {
				totalBytesRead += if (bytesRead != -1L) bytesRead else 0
				progressState.value = (totalBytesRead.toDouble() / contentLength.toDouble()).toFloat()
			}
			return bytesRead
		}
	}
}
