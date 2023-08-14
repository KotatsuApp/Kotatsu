package org.koitharu.kotatsu.core.util.iterator

import org.koitharu.kotatsu.R

class MappingIterator<T, R>(
	private val upstream: Iterator<T>,
	private val mapper: (T) -> R,
) : Iterator<R> {

	override fun hasNext(): Boolean = upstream.hasNext()

	override fun next(): R = mapper(upstream.next())
}
