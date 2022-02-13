package org.koitharu.kotatsu.utils.ext

fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

fun <T> Iterator<T>.toList(): List<T> {
	if (!hasNext()) {
		return emptyList()
	}
	val list = ArrayList<T>()
	while (hasNext()) list += next()
	return list
}

fun <T> Iterator<T>.toSet(): Set<T> {
	if (!hasNext()) {
		return emptySet()
	}
	val list = LinkedHashSet<T>()
	while (hasNext()) list += next()
	return list
}