package org.koitharu.kotatsu.domain.local

import androidx.annotation.WorkerThread
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.utils.ext.sub
import org.koitharu.kotatsu.utils.ext.takeIfReadable
import org.koitharu.kotatsu.utils.ext.toFileNameSafe
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@WorkerThread
class MangaZip(val file: File) {

	private val dir = file.parentFile?.sub(file.name + ".tmp")?.takeIf { it.mkdir() }
		?: throw RuntimeException("Cannot create temporary directory")

	private var index = MangaIndex(null)

	fun prepare(manga: Manga) {
		extract()
		index = MangaIndex(dir.sub(INDEX_ENTRY).takeIfReadable()?.readText())
		index.setMangaInfo(manga, append = true)
	}

	fun cleanup() {
		dir.deleteRecursively()
	}

	fun compress() {
		dir.sub(INDEX_ENTRY).writeText(index.toString())
		ZipOutputStream(file.outputStream()).use { out ->
			for (file in dir.listFiles().orEmpty()) {
				val entry = ZipEntry(file.name)
				out.putNextEntry(entry)
				file.inputStream().use { stream ->
					stream.copyTo(out)
				}
				out.closeEntry()
			}
		}
	}

	private fun extract() {
		if (!file.exists()) {
			return
		}
		ZipInputStream(file.inputStream()).use { input ->
			while (true) {
				val entry = input.nextEntry ?: return
				if (!entry.isDirectory) {
					dir.sub(entry.name).outputStream().use { out ->
						input.copyTo(out)
					}
				}
				input.closeEntry()
			}
		}
	}

	fun addCover(file: File, ext: String) {
		val name = buildString {
			append(FILENAME_PATTERN.format(0, 0))
			if (ext.isNotEmpty() && ext.length <= 4) {
				append('.')
				append(ext)
			}
		}
		file.copyTo(dir.sub(name), overwrite = true)
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
		file.copyTo(dir.sub(name), overwrite = true)
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