package org.koitharu.kotatsu.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.internal.closeQuietly
import okio.Closeable
import org.json.JSONArray
import org.koitharu.kotatsu.core.exceptions.BadBackupFormatException
import java.io.File
import java.util.EnumSet
import java.util.zip.ZipException
import java.util.zip.ZipFile

class BackupZipInput private constructor(val file: File) : Closeable {

	private val zipFile = ZipFile(file)

	suspend fun getEntry(name: BackupEntry.Name): BackupEntry? = runInterruptible(Dispatchers.IO) {
		val entry = zipFile.getEntry(name.key) ?: return@runInterruptible null
		val json = zipFile.getInputStream(entry).use {
			JSONArray(it.bufferedReader().readText())
		}
		BackupEntry(name, json)
	}

	suspend fun entries(): Set<BackupEntry.Name> = runInterruptible(Dispatchers.IO) {
		zipFile.entries().toList().mapNotNullTo(EnumSet.noneOf(BackupEntry.Name::class.java)) { ze ->
			BackupEntry.Name.entries.find { it.key == ze.name }
		}
	}

	override fun close() {
		zipFile.close()
	}

	fun closeAndDelete() {
		closeQuietly()
		file.delete()
	}

	companion object {

		fun from(file: File): BackupZipInput {
			var res: BackupZipInput? = null
			return try {
				res = BackupZipInput(file)
				if (res.zipFile.getEntry("index") == null) {
					throw BadBackupFormatException(null)
				}
				res
			} catch (exception: Throwable) {
				res?.closeQuietly()
				throw if (exception is ZipException) {
					BadBackupFormatException(exception)
				} else {
					exception
				}
			}
		}
	}
}
