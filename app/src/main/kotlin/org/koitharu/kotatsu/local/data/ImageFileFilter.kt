package org.koitharu.kotatsu.local.data

import java.io.File

fun hasImageExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "")
	return ext.equals("png", ignoreCase = true) || ext.equals("jpg", ignoreCase = true)
		|| ext.equals("jpeg", ignoreCase = true) || ext.equals("webp", ignoreCase = true)
}

fun hasImageExtension(file: File) = hasImageExtension(file.name)
