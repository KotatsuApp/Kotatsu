package org.koitharu.kotatsu.local.data

import android.net.Uri
import org.koitharu.kotatsu.core.util.ext.URI_SCHEME_ZIP
import java.io.File

private fun isCbzExtension(ext: String?): Boolean {
	return ext.equals("cbz", ignoreCase = true) || ext.equals("zip", ignoreCase = true)
}

fun hasCbzExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "")
	return isCbzExtension(ext)
}

fun File.hasCbzExtension() = isCbzExtension(extension)

fun Uri.isZipUri() = scheme.let {
	it == URI_SCHEME_ZIP || it == "cbz" || it == "zip"
}

fun Uri.isFileUri() = scheme == "file"
