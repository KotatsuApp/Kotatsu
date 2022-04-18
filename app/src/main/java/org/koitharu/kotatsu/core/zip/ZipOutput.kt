package org.koitharu.kotatsu.core.zip

import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import okio.Closeable
import java.io.File
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

	fun put(name: String, file: File) {
		entryNames.add(name)
		output.appendFile(file, name)
	}

	fun put(name: String, content: String) {
		entryNames.add(name)
		output.appendText(content, name)
	}

	fun addDirectory(name: String) {
		entryNames.add(name)
		val entry = if (name.endsWith("/")) {
			ZipEntry(name)
		} else {
			ZipEntry("$name/")
		}
		output.putNextEntry(entry)
	}

	@WorkerThread
	fun copyEntryFrom(other: ZipFile, entry: ZipEntry): Boolean {
		return if (entryNames.add(entry.name)) {
			val zipEntry = ZipEntry(entry.name)
			output.putNextEntry(zipEntry)
			other.getInputStream(entry).use { input ->
				input.copyTo(output)
			}
			output.closeEntry()
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
}