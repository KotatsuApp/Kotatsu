package org.koitharu.kotatsu.core.zip

import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import okio.Closeable
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.withChildren
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipOutput(
	val file: File,
	private val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
) : Closeable {

	private val entryNames = ArraySet<String>()
	private var cachedOutput: ZipOutputStream? = null
	private var append: Boolean = false

	@Blocking
	fun put(name: String, file: File): Boolean = withOutput { output ->
		output.appendFile(file, name)
	}

	@Blocking
	fun put(name: String, content: String): Boolean = withOutput { output ->
		output.appendText(content, name)
	}

	@Blocking
	fun addDirectory(name: String): Boolean {
		val entry = if (name.endsWith("/")) {
			ZipEntry(name)
		} else {
			ZipEntry("$name/")
		}
		return if (entryNames.add(entry.name)) {
			withOutput { output ->
				output.putNextEntry(entry)
				output.closeEntry()
			}
			true
		} else {
			false
		}
	}

	@Blocking
	fun copyEntryFrom(other: ZipFile, entry: ZipEntry): Boolean {
		return if (entryNames.add(entry.name)) {
			val zipEntry = ZipEntry(entry.name)
			withOutput { output ->
				output.putNextEntry(zipEntry)
				try {
					other.getInputStream(entry).use { input ->
						input.copyTo(output)
					}
				} finally {
					output.closeEntry()
				}
			}
			true
		} else {
			false
		}
	}

	@Blocking
	fun finish() = withOutput { output ->
		output.finish()
	}

	@Synchronized
	override fun close() {
		cachedOutput?.closeSafe()
		cachedOutput = null
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
			fileToZip.withChildren { children ->
				children.forEach { childFile ->
					appendFile(childFile, "$name/${childFile.name}")
				}
			}
		} else {
			FileInputStream(fileToZip).use { fis ->
				if (!entryNames.add(name)) {
					return false
				}
				val zipEntry = ZipEntry(name)
				putNextEntry(zipEntry)
				try {
					fis.copyTo(this)
				} finally {
					closeEntry()
				}
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
		try {
			content.byteInputStream().copyTo(this)
		} finally {
			closeEntry()
		}
		return true
	}

	@Synchronized
	private fun <T> withOutput(block: (ZipOutputStream) -> T): T {
		return try {
			(cachedOutput ?: newOutput(append)).withOutputImpl(block).also {
				append = true // after 1st success write
			}
		} catch (e: NullPointerException) { // probably NullPointerException: Deflater has been closed
			e.printStackTraceDebug()
			newOutput(append).withOutputImpl(block)
		}
	}

	private fun <T> ZipOutputStream.withOutputImpl(block: (ZipOutputStream) -> T): T {
		val res = block(this)
		flush()
		return res
	}

	private fun newOutput(append: Boolean) = ZipOutputStream(FileOutputStream(file, append)).also {
		it.setLevel(compressionLevel)
		cachedOutput?.closeSafe()
		cachedOutput = it
	}

	private fun Closeable.closeSafe() {
		try {
			close()
		} catch (e: NullPointerException) {
			// Don't throw the "Deflater has been closed" exception
			e.printStackTraceDebug()
		}
	}
}
