package org.koitharu.kotatsu.core.util.ext

import org.koitharu.kotatsu.core.util.CompositeRunnable

@Suppress("UNCHECKED_CAST")
fun <T> Class<T>.castOrNull(obj: Any?): T? {
	if (obj == null || !isInstance(obj)) {
		return null
	}
	return obj as T
}

/* CompositeRunnable */

operator fun Runnable.plus(other: Runnable): Runnable {
	val list = ArrayList<Runnable>(this.size + other.size)
	if (this is CompositeRunnable) list.addAll(this) else list.add(this)
	if (other is CompositeRunnable) list.addAll(other) else list.add(other)
	return CompositeRunnable(list)
}

private val Runnable.size: Int
	get() = if (this is CompositeRunnable) size else 1
