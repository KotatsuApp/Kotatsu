package org.koitharu.kotatsu.core.zip

import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
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

	suspend fun put(name: String, file: File): Unit = runInterruptible(Dispatchers.IO) {
		entryNames.add(name)
		output.appendFile(file, name)
	}

	suspend fun put(name: String, content: String): Unit = runInterruptible(Dispatchers.IO) {
		entryNames.add(name)
		output.appendText(content, name)
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

	suspend fun finish() = runInterruptible(Dispatchers.IO) {
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