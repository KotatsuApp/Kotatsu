package org.koitharu.kotatsu.core.util.iterator

import okhttp3.internal.closeQuietly
import okio.Closeable

class CloseableIterator<T>(
	private val upstream: Iterator<T>,
	private val closeable: Closeable,
) : Iterator<T>, Closeable {

	private var isClosed = false

	override fun hasNext(): Boolean {
		val result = upstream.hasNext()
		if (!result) {
			close()
		}
		return result
	}

	override fun next(): T {
		try {
			return upstream.next()
		} catch (e: NoSuchElementException) {
			close()
			throw e
		}
	}

	override fun close() {
		if (!isClosed) {
			closeable.closeQuietly()
			isClosed = true
		}
	}
}
