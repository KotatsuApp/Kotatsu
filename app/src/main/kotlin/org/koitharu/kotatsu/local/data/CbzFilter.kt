package org.koitharu.kotatsu.local.data

import java.io.File

private fun isCbzExtension(ext: String?): Boolean {
	return ext.equals("cbz", ignoreCase = true) || ext.equals("zip", ignoreCase = true)
}

fun hasCbzExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "")
	return isCbzExtension(ext)
}

fun File.hasCbzExtension() = isCbzExtension(extension)
