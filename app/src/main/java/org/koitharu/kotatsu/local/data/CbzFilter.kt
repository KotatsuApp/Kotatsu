package org.koitharu.kotatsu.local.data

import java.io.File
import java.io.FilenameFilter
import java.util.*

class CbzFilter : FilenameFilter {

	override fun accept(dir: File, name: String): Boolean {
		val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
		return ext == "cbz" || ext == "zip"
	}
}