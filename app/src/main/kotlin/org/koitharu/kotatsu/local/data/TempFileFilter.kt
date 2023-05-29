package org.koitharu.kotatsu.local.data

import java.io.File
import java.io.FilenameFilter

class TempFileFilter : FilenameFilter {

	override fun accept(dir: File, name: String): Boolean {
		return name.endsWith(".tmp", ignoreCase = true)
	}
}
