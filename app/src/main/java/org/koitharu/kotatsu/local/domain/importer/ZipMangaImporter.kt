package org.koitharu.kotatsu.local.domain.importer

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.utils.ext.copyToSuspending
import org.koitharu.kotatsu.utils.ext.resolveName
import java.io.File
import java.io.IOException

class ZipMangaImporter(
	storageManager: LocalStorageManager,
) : MangaImporter(storageManager) {

	override suspend fun import(uri: Uri): LocalManga {
		val contentResolver = storageManager.contentResolver
		return withContext(Dispatchers.IO) {
			val name = contentResolver.resolveName(uri) ?: throw IOException("Cannot fetch name from uri: $uri")
			if (!CbzFilter.isFileSupported(name)) {
				throw UnsupportedFileException("Unsupported file on $uri")
			}
			val dest = File(getOutputDir(), name)
			runInterruptible {
				contentResolver.openInputStream(uri)
			}?.use { source ->
				dest.outputStream().use { output ->
					source.copyToSuspending(output)
				}
			} ?: throw IOException("Cannot open input stream: $uri")
			LocalMangaInput.of(dest).getManga()
		}
	}
}
