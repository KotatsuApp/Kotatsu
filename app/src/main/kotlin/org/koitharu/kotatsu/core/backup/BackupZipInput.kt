package org.koitharu.kotatsu.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.Closeable
import org.json.JSONArray
import java.io.File
import java.util.zip.ZipFile

class BackupZipInput(val file: File) : Closeable {

	private val zipFile = ZipFile(file)

	suspend fun getEntry(name: String): BackupEntry? = runInterruptible(Dispatchers.IO) {
		val entry = zipFile.getEntry(name) ?: return@runInterruptible null
		val json = zipFile.getInputStream(entry).use {
			JSONArray(it.bufferedReader().readText())
		}
		BackupEntry(name, json)
	}

	override fun close() {
		zipFile.close()
	}
}
