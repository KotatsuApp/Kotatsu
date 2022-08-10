package org.koitharu.kotatsu.local.domain.importer

import android.net.Uri
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.resolveName

class ZipMangaImporter(
	storageManager: LocalStorageManager,
	private val localMangaRepository: LocalMangaRepository,
) : MangaImporter(storageManager) {

	override suspend fun import(uri: Uri): Manga {
		val contentResolver = storageManager.contentResolver
		return withContext(Dispatchers.IO) {
			val name = contentResolver.resolveName(uri) ?: throw IOException("Cannot fetch name from uri: $uri")
			if (!CbzFilter.isFileSupported(name)) {
				throw UnsupportedFileException("Unsupported file on $uri")
			}
			val dest = File(getOutputDir(), name)
			runInterruptible {
				contentResolver.openInputStream(uri)?.use { source ->
					dest.outputStream().use { output ->
						source.copyTo(output)
					}
				}
			} ?: throw IOException("Cannot open input stream: $uri")
			localMangaRepository.getFromFile(dest)
		}
	}
}
