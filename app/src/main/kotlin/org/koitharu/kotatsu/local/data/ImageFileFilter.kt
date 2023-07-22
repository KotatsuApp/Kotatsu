package org.koitharu.kotatsu.local.data

import java.util.zip.ZipEntry

fun isImageExtension(string: String): Boolean {
	return string.equals("png", ignoreCase = true) || string.equals("jpg", ignoreCase = true)
		|| string.equals("jpeg", ignoreCase = true) || string.equals("webp", ignoreCase = true)
}

fun hasImageExtension(name: String): Boolean {
	return isImageExtension(name.substringAfterLast('.', ""))
}

fun hasImageExtension(entry: ZipEntry): Boolean {
	return hasImageExtension(entry.name)
}
