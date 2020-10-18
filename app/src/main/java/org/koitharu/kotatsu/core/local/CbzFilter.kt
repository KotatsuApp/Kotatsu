package org.koitharu.kotatsu.core.local

import java.io.File
import java.io.FilenameFilter
import java.util.*

class CbzFilter : FilenameFilter {

	override fun accept(dir: File, name: String): Boolean {
		val ext = name.substringAfterLast('.', "").toLowerCase(Locale.ROOT)
		return ext == "cbz" || ext == "zip"
	}
}