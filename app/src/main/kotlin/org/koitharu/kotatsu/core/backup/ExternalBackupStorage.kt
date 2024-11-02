package org.koitharu.kotatsu.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okio.IOException
import okio.buffer
import okio.sink
import okio.source
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.prefs.AppSettings
import java.io.File
import javax.inject.Inject

class ExternalBackupStorage @Inject constructor(
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) {

	suspend fun list(): List<BackupFile> = runInterruptible(Dispatchers.IO) {
		getRoot().listFiles().mapNotNull {
			if (it.isFile && it.canRead()) {
				BackupFile(
					uri = it.uri,
					dateTime = it.name?.let { fileName ->
						BackupZipOutput.parseBackupDateTime(fileName)
					} ?: return@mapNotNull null,
				)
			} else {
				null
			}
		}.sortedDescending()
	}

	suspend fun put(file: File): Uri = runInterruptible(Dispatchers.IO) {
		val out = checkNotNull(getRoot().createFile("application/zip", file.nameWithoutExtension)) {
			"Cannot create target backup file"
		}
		checkNotNull(context.contentResolver.openOutputStream(out.uri, "wt")).sink().use { sink ->
			file.source().buffer().use { src ->
				src.readAll(sink)
			}
		}
		out.uri
	}

	suspend fun delete(victim: BackupFile) = runInterruptible(Dispatchers.IO) {
		val df = checkNotNull(DocumentFile.fromSingleUri(context, victim.uri)) {
			"${victim.uri} cannot be resolved to the DocumentFile"
		}
		if (!df.delete()) {
			throw IOException("Cannot delete ${df.uri}")
		}
	}

	suspend fun getLastBackupDate() = list().maxByOrNull { it.dateTime }?.dateTime

	suspend fun trim(maxCount: Int) {
		list().drop(maxCount).forEach {
			delete(it)
		}
	}

	@Blocking
	private fun getRoot(): DocumentFile {
		val uri = checkNotNull(settings.periodicalBackupDirectory) {
			"Backup directory is not specified"
		}
		val root = DocumentFile.fromTreeUri(context, uri)
		return checkNotNull(root) { "Cannot obtain DocumentFile from $uri" }
	}
}
