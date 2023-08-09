package org.koitharu.kotatsu.local.data

import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter

class TempFileFilter : FilenameFilter, FileFilter {

	override fun accept(dir: File, name: String): Boolean {
		return name.endsWith(".tmp", ignoreCase = true)
	}

	override fun accept(file: File): Boolean {
		return file.name.endsWith(".tmp", ignoreCase = true)
	}
}
