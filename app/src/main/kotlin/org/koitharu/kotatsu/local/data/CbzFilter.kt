package org.koitharu.kotatsu.local.data

import android.net.Uri
import java.io.File

private fun isCbzExtension(ext: String?): Boolean {
	return ext.equals("cbz", ignoreCase = true) || ext.equals("zip", ignoreCase = true)
}

fun hasCbzExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "")
	return isCbzExtension(ext)
}

fun hasCbzExtension(file: File) = isCbzExtension(file.name)

fun isCbzUri(uri: Uri) = isCbzExtension(uri.scheme)
