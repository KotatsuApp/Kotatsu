package org.koitharu.kotatsu.domain.local

import androidx.annotation.CheckResult
import androidx.annotation.WorkerThread
import org.koitharu.kotatsu.core.local.WritableCbzFile
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import org.koitharu.kotatsu.utils.ext.toFileNameSafe
import java.io.File

@WorkerThread
class MangaZip(val file: File) {

	private val writableCbz = WritableCbzFile(file)

	private var index = MangaIndex(null)

	suspend fun prepare(manga: Manga) {
		writableCbz.prepare()
		index = MangaIndex(writableCbz[INDEX_ENTRY].takeIfReadable()?.readText())
		index.setMangaInfo(manga, append = true)
	}

	suspend fun cleanup() {
		writableCbz.cleanup()
	}

	@CheckResult
	suspend fun compress(): Boolean {
		writableCbz[INDEX_ENTRY].writeText(index.toString())
		return writableCbz.flush()
	}

	fun addCover(file: File, ext: String) {
		val name = buildString {
			append(FILENAME_PATTERN.format(0, 0))
			if (ext.isNotEmpty() && ext.length <= 4) {
				append('.')
				append(ext)
			}
		}
		writableCbz[name] = file
		index.setCoverEntry(name)
	}

	fun addPage(chapter: MangaChapter, file: File, pageNumber: Int, ext: String) {
		val name = buildString {
			append(FILENAME_PATTERN.format(chapter.number, pageNumber))
			if (ext.isNotEmpty() && ext.length <= 4) {
				append('.')
				append(ext)
			}
		}
		writableCbz[name] = file
		index.addChapter(chapter)
	}

	companion object {

		private const val FILENAME_PATTERN = "%03d%03d"

		const val INDEX_ENTRY = "index.json"

		fun findInDir(root: File, manga: Manga): MangaZip {
			val name = manga.title.toFileNameSafe() + ".cbz"
			val file = File(root, name)
			return MangaZip(file)
		}
	}
}