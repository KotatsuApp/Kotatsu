package org.koitharu.kotatsu.local.domain.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.local.data.LocalStorageManager
import java.io.File
import java.io.IOException
import javax.inject.Inject

abstract class MangaImporter(
	protected val storageManager: LocalStorageManager,
) {

	abstract suspend fun import(uri: Uri): LocalManga

	suspend fun getOutputDir(): File {
		return storageManager.getDefaultWriteableDir() ?: throw IOException("External files dir unavailable")
	}

	class Factory @Inject constructor(
		@ApplicationContext private val context: Context,
		private val storageManager: LocalStorageManager,
	) {

		fun create(uri: Uri): MangaImporter {
			return when {
				isDir(uri) -> DirMangaImporter(context, storageManager)
				else -> ZipMangaImporter(storageManager)
			}
		}

		private fun isDir(uri: Uri): Boolean {
			return runCatching {
				DocumentFile.fromTreeUri(context, uri)
			}.isSuccess
		}
	}
}
