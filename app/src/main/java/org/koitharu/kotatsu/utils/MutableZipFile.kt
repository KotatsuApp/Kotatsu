package org.koitharu.kotatsu.utils

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

open class MutableZipFile(val file: File) {

	protected val dir = File(file.parentFile, file.nameWithoutExtension)

	suspend fun unpack(): Unit = runInterruptible(Dispatchers.IO) {
		check(dir.list().isNullOrEmpty()) {
			"Dir ${dir.name} is not empty"
		}
		if (!dir.exists()) {
			dir.mkdir()
		}
		if (!file.exists()) {
			return@runInterruptible
		}
		ZipInputStream(FileInputStream(file)).use { zip ->
			var entry = zip.nextEntry
			while (entry != null) {
				val target = File(dir.path + File.separator + entry.name)
				target.parentFile?.mkdirs()
				target.outputStream().use { out ->
					zip.copyTo(out)
				}
				zip.closeEntry()
				entry = zip.nextEntry
			}
		}
	}

	suspend fun cleanup() = withContext(Dispatchers.IO) {
		dir.deleteRecursively()
	}

	suspend fun flush(): Boolean = runInterruptible(Dispatchers.IO) {
		val tempFile = File(file.path + ".tmp")
		if (tempFile.exists()) {
			tempFile.delete()
		}
		try {
			ZipOutputStream(FileOutputStream(tempFile)).use { zip ->
				dir.listFiles()?.forEach {
					zipFile(it, it.name, zip)
				}
				zip.flush()
			}
			tempFile.renameTo(file)
		} finally {
			if (tempFile.exists()) {
				tempFile.delete()
			}
		}
	}

	operator fun get(name: String) = File(dir, name)

	suspend fun put(name: String, file: File): Unit = withContext(Dispatchers.IO) {
		file.copyTo(this@MutableZipFile[name], overwrite = true)
	}

	suspend fun put(name: String, data: String): Unit = withContext(Dispatchers.IO) {
		this@MutableZipFile[name].writeText(data)
	}

	suspend fun getContent(name: String): String = withContext(Dispatchers.IO) {
		get(name).readText()
	}

	companion object {

		@WorkerThread
		private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
			if (fileToZip.isDirectory) {
				if (fileName.endsWith("/")) {
					zipOut.putNextEntry(ZipEntry(fileName))
				} else {
					zipOut.putNextEntry(ZipEntry("$fileName/"))
				}
				zipOut.closeEntry()
				fileToZip.listFiles()?.forEach { childFile ->
					zipFile(childFile, "$fileName/${childFile.name}", zipOut)
				}
			} else {
				FileInputStream(fileToZip).use { fis ->
					val zipEntry = ZipEntry(fileName)
					zipOut.putNextEntry(zipEntry)
					fis.copyTo(zipOut)
				}
			}
		}
	}
}