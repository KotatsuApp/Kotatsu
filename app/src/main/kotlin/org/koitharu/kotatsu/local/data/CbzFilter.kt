package org.koitharu.kotatsu.local.data

import java.io.File

private fun isZipExtension(ext: String?): Boolean {
	return ext.equals("cbz", ignoreCase = true) || ext.equals("zip", ignoreCase = true)
}

fun hasZipExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "")
	return isZipExtension(ext)
}

val File.isZipArchive: Boolean
	get() = isFile && isZipExtension(extension)
