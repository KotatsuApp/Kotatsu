package org.koitharu.kotatsu.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okio.Buffer
import okio.ForwardingSource
import okio.Source

class CancellableSource(
	private val job: Job?,
	delegate: Source,
) : ForwardingSource(delegate) {

	override fun read(sink: Buffer, byteCount: Long): Long {
		job?.ensureActive()
		return super.read(sink, byteCount)
	}
}
