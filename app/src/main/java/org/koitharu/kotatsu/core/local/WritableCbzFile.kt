package org.koitharu.kotatsu.core.local

import androidx.annotation.CheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WritableCbzFile(private val file: File) {

	private val dir = File(file.parentFile, file.nameWithoutExtension)

	@Suppress("BlockingMethodInNonBlockingContext")
	suspend fun prepare() = withContext(Dispatchers.IO) {
		check(dir.list().isNullOrEmpty()) {
			"Dir ${dir.name} is not empty"
		}
		if (!dir.exists()) {
			dir.mkdir()
		}
		if (!file.exists()) {
			return@withContext
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

	@CheckResult
	@Suppress("BlockingMethodInNonBlockingContext")
	suspend fun flush() = withContext(Dispatchers.IO) {
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

	operator fun set(name: String, file: File) {
		file.copyTo(this[name], overwrite = true)
	}

	companion object {

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