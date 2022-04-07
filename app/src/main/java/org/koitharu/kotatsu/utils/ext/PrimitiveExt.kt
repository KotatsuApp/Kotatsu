package org.koitharu.kotatsu.utils.ext

inline fun Int.ifZero(defaultValue: () -> Int): Int = if (this == 0) defaultValue() else this