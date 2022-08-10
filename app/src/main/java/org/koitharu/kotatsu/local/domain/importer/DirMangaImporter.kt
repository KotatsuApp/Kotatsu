package org.koitharu.kotatsu.local.domain.importer

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.domain.CbzMangaOutput
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.longOf

// TODO: Add support for chapters in cbz
// https://github.com/KotatsuApp/Kotatsu/issues/31
class DirMangaImporter(
	private val context: Context,
	storageManager: LocalStorageManager,
	private val localMangaRepository: LocalMangaRepository,
) : MangaImporter(storageManager) {

	private val contentResolver = context.contentResolver

	override suspend fun import(uri: Uri): Manga {
		val root = requireNotNull(DocumentFile.fromTreeUri(context, uri)) {
			"Provided uri $uri is not a tree"
		}
		val manga = Manga(root)
		val output = CbzMangaOutput.get(getOutputDir(), manga)
		try {
			val dest = output.use {
				addPages(
					output = it,
					root = root,
					path = "",
					state = State(uri.hashCode(), 0, false),
				)
				it.sortChaptersByName()
				it.mergeWithExisting()
				it.finalize()
				it.file
			}
			return localMangaRepository.getFromFile(dest)
		} finally {
			withContext(NonCancellable) {
				output.cleanup()
				File(getOutputDir(), "page.tmp").deleteAwait()
			}
		}
	}

	private suspend fun addPages(output: CbzMangaOutput, root: DocumentFile, path: String, state: State) {
		var number = 0
		for (file in root.listFiles()) {
			when {
				file.isDirectory -> {
					addPages(output, file, path + "/" + file.name, state)
				}
				file.isFile -> {
					val tempFile = file.asTempFile()
					if (!state.hasCover) {
						output.addCover(tempFile, file.extension)
						state.hasCover = true
					}
					output.addPage(
						chapter = state.getChapter(path),
						file = tempFile,
						pageNumber = number,
						ext = file.extension,
					)
					number++
				}
			}
		}
	}

	private suspend fun DocumentFile.asTempFile(): File {
		val file = File(getOutputDir(), "page.tmp")
		checkNotNull(contentResolver.openInputStream(uri)) {
			"Cannot open input stream for $uri"
		}.use { input ->
			file.outputStream().use { output ->
				input.copyTo(output)
			}
		}
		return file
	}

	private fun Manga(file: DocumentFile) = Manga(
		id = longOf(file.uri.hashCode(), 0),
		title = checkNotNull(file.name),
		altTitle = null,
		url = file.uri.path.orEmpty(),
		publicUrl = file.uri.toString(),
		rating = RATING_UNKNOWN,
		isNsfw = false,
		coverUrl = "",
		tags = emptySet(),
		state = null,
		author = null,
		source = MangaSource.LOCAL,
	)

	private val DocumentFile.extension: String
		get() = type?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
			?: name?.substringAfterLast('.')?.takeIf { it.length in 2..4 }
			?: error("Cannot obtain extension of $uri")

	private class State(
		private val rootId: Int,
		private var counter: Int,
		var hasCover: Boolean,
	) {

		private val chapters = HashMap<String, MangaChapter>()

		@Synchronized
		fun getChapter(path: String): MangaChapter {
			return chapters.getOrPut(path) {
				counter++
				MangaChapter(
					id = longOf(rootId, counter),
					name = path.replace('/', ' ').trim(),
					number = counter,
					url = path.ifEmpty { "Default chapter" },
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = MangaSource.LOCAL,
				)
			}
		}
	}
}
