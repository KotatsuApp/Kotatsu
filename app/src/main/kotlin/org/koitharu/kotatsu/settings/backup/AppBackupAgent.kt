package org.koitharu.kotatsu.settings.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.core.backup.BackupEntry
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.BackupZipInput
import org.koitharu.kotatsu.core.backup.BackupZipOutput
import org.koitharu.kotatsu.core.db.MangaDatabase
import java.io.*

class AppBackupAgent : BackupAgent() {

	override fun onBackup(
		oldState: ParcelFileDescriptor?,
		data: BackupDataOutput?,
		newState: ParcelFileDescriptor?
	) = Unit

	override fun onRestore(
		data: BackupDataInput?,
		appVersionCode: Int,
		newState: ParcelFileDescriptor?
	) = Unit

	override fun onFullBackup(data: FullBackupDataOutput) {
		super.onFullBackup(data)
		val file = createBackupFile(this, BackupRepository(MangaDatabase(applicationContext)))
		try {
			fullBackupFile(file, data)
		} finally {
			file.delete()
		}
	}

	override fun onRestoreFile(
		data: ParcelFileDescriptor,
		size: Long,
		destination: File?,
		type: Int,
		mode: Long,
		mtime: Long
	) {
		if (destination?.name?.endsWith(".bk.zip") == true) {
			restoreBackupFile(data.fileDescriptor, size, BackupRepository(MangaDatabase(applicationContext)))
			destination.delete()
		} else {
			super.onRestoreFile(data, size, destination, type, mode, mtime)
		}
	}

	@VisibleForTesting
	fun createBackupFile(context: Context, repository: BackupRepository) = runBlocking {
		BackupZipOutput(context).use { backup ->
			backup.put(repository.createIndex())
			backup.put(repository.dumpHistory())
			backup.put(repository.dumpCategories())
			backup.put(repository.dumpFavourites())
			backup.finish()
			backup.file
		}
	}

	@VisibleForTesting
	fun restoreBackupFile(fd: FileDescriptor, size: Long, repository: BackupRepository) {
		val tempFile = File.createTempFile("backup_", ".tmp")
		FileInputStream(fd).use { input ->
			tempFile.outputStream().use { output ->
				input.copyLimitedTo(output, size)
			}
		}
		val backup = BackupZipInput(tempFile)
		try {
			runBlocking {
				repository.restoreHistory(backup.getEntry(BackupEntry.HISTORY))
				repository.restoreCategories(backup.getEntry(BackupEntry.CATEGORIES))
				repository.restoreFavourites(backup.getEntry(BackupEntry.FAVOURITES))
			}
		} finally {
			backup.close()
			tempFile.delete()
		}
	}

	private fun InputStream.copyLimitedTo(out: OutputStream, limit: Long) {
		var bytesCopied: Long = 0
		val buffer = ByteArray(DEFAULT_BUFFER_SIZE.coerceAtMost(limit.toInt()))
		var bytes = read(buffer)
		while (bytes >= 0) {
			out.write(buffer, 0, bytes)
			bytesCopied += bytes
			val bytesLeft = (limit - bytesCopied).toInt()
			if (bytesLeft <= 0) {
				break
			}
			bytes = read(buffer, 0, buffer.size.coerceAtMost(bytesLeft))
		}
	}
}