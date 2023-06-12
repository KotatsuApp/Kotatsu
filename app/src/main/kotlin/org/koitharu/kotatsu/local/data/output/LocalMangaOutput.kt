package org.koitharu.kotatsu.local.data.output

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Closeable
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
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

	abstract suspend fun flushChapter(chapter: MangaChapter): Boolean

	abstract suspend fun finish()

	abstract suspend fun cleanup()

	companion object {

		const val ENTRY_NAME_INDEX = "index.json"
		const val SUFFIX_TMP = ".tmp"
		private val mutex = Mutex()

		suspend fun getOrCreate(root: File, manga: Manga): LocalMangaOutput = withContext(Dispatchers.IO) {
			val preferSingleCbz = manga.chapters.let {
				it != null && it.size <= 3
			}
			checkNotNull(getImpl(root, manga, onlyIfExists = false, preferSingleCbz))
		}

		suspend fun get(root: File, manga: Manga): LocalMangaOutput? = withContext(Dispatchers.IO) {
			getImpl(root, manga, onlyIfExists = true, preferSingleCbz = false)
		}

		private suspend fun getImpl(
			root: File,
			manga: Manga,
			onlyIfExists: Boolean,
			preferSingleCbz: Boolean,
		): LocalMangaOutput? {
			mutex.withLock {
				var i = 0
				val baseName = manga.title.toFileNameSafe()
				while (true) {
					val fileName = if (i == 0) baseName else baseName + "_$i"
					val dir = File(root, fileName)
					val zip = File(root, "$fileName.cbz")
					i++
					return when {
						dir.isDirectory -> {
							if (canWriteTo(dir, manga)) {
								LocalMangaDirOutput(dir, manga)
							} else {
								continue
							}
						}

						zip.isFile -> if (canWriteTo(zip, manga)) {
							LocalMangaZipOutput(zip, manga)
						} else {
							continue
						}

						!onlyIfExists -> if (preferSingleCbz) {
							LocalMangaZipOutput(zip, manga)
						} else {
							LocalMangaDirOutput(dir, manga)
						}

						else -> null
					}
				}
			}
		}

		private suspend fun canWriteTo(file: File, manga: Manga): Boolean {
			val info = LocalMangaInput.of(file).getMangaInfo() ?: return false
			return info.id == manga.id
		}
	}
}
