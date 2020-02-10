package org.koitharu.kotatsu.core.local

import java.io.File
import java.io.FilenameFilter

class CbzFilter : FilenameFilter {

	override fun accept(dir: File, name: String) = name.endsWith(".cbz", ignoreCase = true)
}