package org.koitharu.kotatsu.local.data

import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.Locale
import java.util.zip.ZipEntry

class ImageFileFilter : FilenameFilter, FileFilter {

	override fun accept(dir: File, name: String): Boolean {
		val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
		return isExtensionValid(ext)
	}

	override fun accept(pathname: File?): Boolean {
		val ext = pathname?.extension?.lowercase(Locale.ROOT) ?: return false
		return isExtensionValid(ext)
	}

	fun accept(entry: ZipEntry): Boolean {
		val ext = entry.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
		return isExtensionValid(ext)
	}

	fun isExtensionValid(ext: String): Boolean {
		return ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp"
	}
}
