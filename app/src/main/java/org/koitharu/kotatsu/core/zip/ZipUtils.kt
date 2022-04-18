package org.koitharu.kotatsu.core.zip

import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@WorkerThread
fun ZipOutputStream.appendFile(fileToZip: File, name: String) {
	if (fileToZip.isDirectory) {
		if (name.endsWith("/")) {
			putNextEntry(ZipEntry(name))
		} else {
			putNextEntry(ZipEntry("$name/"))
		}
		closeEntry()
		fileToZip.listFiles()?.forEach { childFile ->
			appendFile(childFile, "$name/${childFile.name}")
		}
	} else {
		FileInputStream(fileToZip).use { fis ->
			val zipEntry = ZipEntry(name)
			putNextEntry(zipEntry)
			fis.copyTo(this)
			closeEntry()
		}
	}
}

@WorkerThread
fun ZipOutputStream.appendText(content: String, name: String) {
	val zipEntry = ZipEntry(name)
	putNextEntry(zipEntry)
	content.byteInputStream().copyTo(this)
	closeEntry()
}