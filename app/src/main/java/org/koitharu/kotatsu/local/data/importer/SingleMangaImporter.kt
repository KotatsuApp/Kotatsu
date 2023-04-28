package org.koitharu.kotatsu.local.data.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.utils.ext.copyToSuspending
import org.koitharu.kotatsu.utils.ext.resolveName
import java.io.File
import java.io.IOException
import javax.inject.Inject

@Reusable
class SingleMangaImporter @Inject constructor(
	@ApplicationContext private val context: Context,
	private val storageManager: LocalStorageManager,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
) {

	private val contentResolver = context.contentResolver

	suspend fun import(uri: Uri, progressState: MutableStateFlow<Float>?): LocalManga {
		val result = if (isDirectory(uri)) {
			importDirectory(uri, progressState)
		} else {
			importFile(uri, progressState)
		}
		localStorageChanges.emit(result)
		return result
	}

	private suspend fun importFile(uri: Uri, progressState: MutableStateFlow<Float>?): LocalManga {
		val contentResolver = storageManager.contentResolver
		val name = contentResolver.resolveName(uri) ?: throw IOException("Cannot fetch name from uri: $uri")
		if (!CbzFilter.isFileSupported(name)) {
			throw UnsupportedFileException("Unsupported file on $uri")
		}
		val dest = File(getOutputDir(), name)
		runInterruptible {
			contentResolver.openInputStream(uri)
		}?.use { source ->
			dest.outputStream().use { output ->
				source.copyToSuspending(output, progressState = progressState)
			}
		} ?: throw IOException("Cannot open input stream: $uri")
		return LocalMangaInput.of(dest).getManga()
	}

	private suspend fun importDirectory(uri: Uri, progressState: MutableStateFlow<Float>?): LocalManga {
		val root = requireNotNull(DocumentFile.fromTreeUri(context, uri)) {
			"Provided uri $uri is not a tree"
		}
		val dest = File(getOutputDir(), root.requireName())
		dest.mkdir()
		for (docFile in root.listFiles()) {
			docFile.copyTo(dest)
		}
		return LocalMangaInput.of(dest).getManga()
	}

	/**
	 * TODO: progress
	 */
	private suspend fun DocumentFile.copyTo(destDir: File) {
		if (isDirectory) {
			val subDir = File(destDir, requireName())
			subDir.mkdir()
			for (docFile in listFiles()) {
				docFile.copyTo(subDir)
			}
		} else {
			inputStream().use { input ->
				File(destDir, requireName()).outputStream().use { output ->
					input.copyToSuspending(output)
				}
			}
		}
	}

	private suspend fun getOutputDir(): File {
		return storageManager.getDefaultWriteableDir() ?: throw IOException("External files dir unavailable")
	}

	private suspend fun DocumentFile.inputStream() = runInterruptible(Dispatchers.IO) {
		contentResolver.openInputStream(uri) ?: throw IOException("Cannot open input stream: $uri")
	}

	private fun DocumentFile.requireName(): String {
		return name ?: throw IOException("Cannot fetch name from uri: $uri")
	}

	private fun isDirectory(uri: Uri): Boolean {
		return runCatching {
			DocumentFile.fromTreeUri(context, uri)
		}.isSuccess
	}
}
