package org.koitharu.kotatsu.backups.domain

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.VisibleForTesting
import com.google.common.io.ByteStreams
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.backups.data.BackupRepository
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.filter.data.SavedFiltersRepository
import org.koitharu.kotatsu.reader.data.TapGridSettings
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.util.EnumSet
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
		val file = createBackupFile(
			this,
			BackupRepository(
				database = MangaDatabase(context = applicationContext),
				settings = AppSettings(applicationContext),
				tapGridSettings = TapGridSettings(applicationContext),
				mangaSourcesRepository = MangaSourcesRepository(
					context = applicationContext,
					db = MangaDatabase(context = applicationContext),
					settings = AppSettings(applicationContext),
				),
				savedFiltersRepository = SavedFiltersRepository(
					context = applicationContext,
				),
			),
		)
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
			restoreBackupFile(
				data.fileDescriptor,
				size,
				BackupRepository(
					database = MangaDatabase(applicationContext),
					settings = AppSettings(applicationContext),
					tapGridSettings = TapGridSettings(applicationContext),
					mangaSourcesRepository = MangaSourcesRepository(
						context = applicationContext,
						db = MangaDatabase(context = applicationContext),
						settings = AppSettings(applicationContext),
					),
					savedFiltersRepository = SavedFiltersRepository(
						context = applicationContext,
					),
				),
			)
			destination.delete()
		} else {
			super.onRestoreFile(data, size, destination, type, mode, mtime)
		}
	}

	@VisibleForTesting
	fun createBackupFile(context: Context, repository: BackupRepository): File {
		val file = BackupUtils.createTempFile(context)
		ZipOutputStream(file.outputStream()).use { output ->
			runBlocking {
				repository.createBackup(output, null)
			}
		}
		return file
	}

	@VisibleForTesting
	fun restoreBackupFile(fd: FileDescriptor, size: Long, repository: BackupRepository) {
		ZipInputStream(ByteStreams.limit(FileInputStream(fd), size)).use { input ->
			val sections = EnumSet.allOf(BackupSection::class.java)
			// managed externally
			sections.remove(BackupSection.SETTINGS)
			sections.remove(BackupSection.SETTINGS_READER_GRID)
			runBlocking {
				repository.restoreBackup(input, sections, null)
			}
		}
	}
}
