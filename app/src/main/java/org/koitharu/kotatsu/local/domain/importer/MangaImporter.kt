package org.koitharu.kotatsu.local.domain.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga

abstract class MangaImporter(
	protected val storageManager: LocalStorageManager,
) {

	abstract suspend fun import(uri: Uri): Manga

	suspend fun getOutputDir(): File {
		return storageManager.getDefaultWriteableDir() ?: throw IOException("External files dir unavailable")
	}

	class Factory @Inject constructor(
		@ApplicationContext private val context: Context,
		private val storageManager: LocalStorageManager,
		private val localMangaRepository: LocalMangaRepository,
	) {

		fun create(uri: Uri): MangaImporter {
			return when {
				isDir(uri) -> DirMangaImporter(context, storageManager, localMangaRepository)
				else -> ZipMangaImporter(storageManager, localMangaRepository)
			}
		}

		private fun isDir(uri: Uri): Boolean {
			return runCatching {
				DocumentFile.fromTreeUri(context, uri)
			}.isSuccess
		}
	}
}
