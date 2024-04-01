package org.koitharu.kotatsu.core.backup

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import okio.Closeable
import org.json.JSONArray
import org.koitharu.kotatsu.core.exceptions.BadBackupFormatException
import org.koitharu.kotatsu.core.util.ext.processLifecycleScope
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

	fun cleanupAsync() {
		processLifecycleScope.launch(Dispatchers.IO, CoroutineStart.ATOMIC) {
			runCatching {
				close()
				file.delete()
			}
		}
	}

	companion object {

		fun from(file: File): BackupZipInput = try {
			val res = BackupZipInput(file)
			if (res.zipFile.getEntry("index") == null) {
				throw BadBackupFormatException(null)
			}
			res
		} catch (e: ZipException) {
			throw BadBackupFormatException(e)
		}
	}
}
