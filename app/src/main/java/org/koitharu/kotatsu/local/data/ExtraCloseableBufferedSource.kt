package org.koitharu.kotatsu.local.data

import okhttp3.internal.closeQuietly
import okio.BufferedSource
import okio.Closeable

class ExtraCloseableBufferedSource(
	private val delegate: BufferedSource,
	vararg closeable: Closeable,
) : BufferedSource by delegate {

	private val extraCloseable = closeable

	override fun close() {
		delegate.close()
		extraCloseable.forEach { x -> x.closeQuietly() }
	}
}