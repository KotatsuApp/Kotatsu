package org.koitharu.kotatsu.local.data

import android.net.Uri
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.Locale

class CbzFilter : FileFilter, FilenameFilter {

	override fun accept(dir: File, name: String): Boolean {
		return isFileSupported(name)
	}

	override fun accept(pathname: File?): Boolean {
		return isFileSupported(pathname?.name ?: return false)
	}

	companion object {

		fun isFileSupported(name: String): Boolean {
			val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
			return ext == "cbz" || ext == "zip"
		}

		fun isUriSupported(uri: Uri): Boolean {
			val scheme = uri.scheme?.lowercase(Locale.ROOT)
			return scheme != null && scheme == "cbz" || scheme == "zip"
		}
	}
}
