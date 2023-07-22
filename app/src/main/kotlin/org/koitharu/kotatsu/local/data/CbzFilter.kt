package org.koitharu.kotatsu.local.data

import android.net.Uri

fun isCbzExtension(string: String): Boolean {
	return string.equals("cbz", ignoreCase = true) || string.equals("zip", ignoreCase = true)
}

fun hasCbzExtension(name: String): Boolean {
	return isCbzExtension(name.substringAfterLast('.', ""))
}

fun hasCbzExtension(uri: Uri) = uri.scheme?.let { isCbzExtension(it) } ?: false
