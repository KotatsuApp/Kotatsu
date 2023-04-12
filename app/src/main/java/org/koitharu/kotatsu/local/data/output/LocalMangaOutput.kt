package org.koitharu.kotatsu.local.data.output

import okio.Closeable
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.toFileNameSafe
import java.io.File

sealed class LocalMangaOutput(
	val rootFile: File,
) : Closeable {

	abstract suspend fun mergeWithExisting()

	abstract suspend fun addCover(file: File, ext: String)

	abstract suspend fun addPage(chapter: MangaChapter, file: File, pageNumber: Int, ext: String)

	abstract suspend fun flushChapter(chapter: MangaChapter)

	abstract suspend fun finish()

	abstract suspend fun cleanup()

	companion object {

		const val ENTRY_NAME_INDEX = "index.json"
		const val SUFFIX_TMP = ".tmp"

		fun getOrCreate(root: File, manga: Manga): LocalMangaOutput {
			return checkNotNull(getImpl(root, manga, onlyIfExists = false))
		}

		fun get(root: File, manga: Manga): LocalMangaOutput? {
			return getImpl(root, manga, onlyIfExists = true)
		}

		private fun getImpl(root: File, manga: Manga, onlyIfExists: Boolean): LocalMangaOutput? {
			val name = manga.title.toFileNameSafe()
			val file = File(root, name)
			return if (file.exists()) {
				if (file.isDirectory) {
					LocalMangaDirOutput(file, manga)
				} else {
					LocalMangaZipOutput(file, manga)
				}
			} else {
				if (onlyIfExists) {
					null
				} else {
					LocalMangaDirOutput(file, manga)
				}
			}
		}
	}
}
