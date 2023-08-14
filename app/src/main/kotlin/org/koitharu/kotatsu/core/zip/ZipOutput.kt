package org.koitharu.kotatsu.core.zip

import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import okio.Closeable
import org.koitharu.kotatsu.core.util.ext.children
import java.io.File
import java.io.FileInputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipOutput(
	val file: File,
	compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
) : Closeable {

	private val entryNames = ArraySet<String>()
	private var isClosed = false
	private val output = ZipOutputStream(file.outputStream()).apply {
		setLevel(compressionLevel)
	}

	@WorkerThread
	fun put(name: String, file: File): Boolean {
		return output.appendFile(file, name)
	}

	@WorkerThread
	fun put(name: String, content: String): Boolean {
		return output.appendText(content, name)
	}

	@WorkerThread
	fun addDirectory(name: String): Boolean {
		val entry = if (name.endsWith("/")) {
			ZipEntry(name)
		} else {
			ZipEntry("$name/")
		}
		return if (entryNames.add(entry.name)) {
			output.putNextEntry(entry)
			output.closeEntry()
			true
		} else {
			false
		}
	}

	@WorkerThread
	fun copyEntryFrom(other: ZipFile, entry: ZipEntry): Boolean {
		return if (entryNames.add(entry.name)) {
			val zipEntry = ZipEntry(entry.name)
			output.putNextEntry(zipEntry)
			try {
				other.getInputStream(entry).use { input ->
					input.copyTo(output)
				}
			} finally {
				output.closeEntry()
			}
			true
		} else {
			false
		}
	}

	fun finish() {
		output.finish()
		output.flush()
	}

	override fun close() {
		if (!isClosed) {
			output.close()
			isClosed = true
		}
	}

	@WorkerThread
	private fun ZipOutputStream.appendFile(fileToZip: File, name: String): Boolean {
		if (fileToZip.isDirectory) {
			val entry = if (name.endsWith("/")) {
				ZipEntry(name)
			} else {
				ZipEntry("$name/")
			}
			if (!entryNames.add(entry.name)) {
				return false
			}
			putNextEntry(entry)
			closeEntry()
			fileToZip.children().forEach { childFile ->
				appendFile(childFile, "$name/${childFile.name}")
			}
		} else {
			FileInputStream(fileToZip).use { fis ->
				if (!entryNames.add(name)) {
					return false
				}
				val zipEntry = ZipEntry(name)
				putNextEntry(zipEntry)
				fis.copyTo(this)
				closeEntry()
			}
		}
		return true
	}

	@WorkerThread
	private fun ZipOutputStream.appendText(content: String, name: String): Boolean {
		if (!entryNames.add(name)) {
			return false
		}
		val zipEntry = ZipEntry(name)
		putNextEntry(zipEntry)
		content.byteInputStream().copyTo(this)
		closeEntry()
		return true
	}
}
