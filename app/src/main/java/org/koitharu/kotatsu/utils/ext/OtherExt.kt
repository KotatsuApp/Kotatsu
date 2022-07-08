package org.koitharu.kotatsu.utils.ext

import android.icu.lang.UCharacter.GraphemeClusterBreak.T

@Suppress("UNCHECKED_CAST")
fun <T> Class<T>.castOrNull(obj: Any?): T? {
	if (obj == null || !isInstance(obj)) {
		return null
	}
	return obj as T
}