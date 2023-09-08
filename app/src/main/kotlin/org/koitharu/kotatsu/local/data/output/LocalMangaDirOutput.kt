package org.koitharu.kotatsu.local.data.output

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.model.findById
import org.koitharu.kotatsu.core.util.ext.deleteAwait
import org.koitharu.kotatsu.core.util.ext.takeIfReadable
import org.koitharu.kotatsu.core.zip.ZipOutput
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.toFileNameSafe
import java.io.File

class LocalMangaDirOutput(
	rootFile: File,
	manga: Manga,
) : LocalMangaOutput(rootFile) {

	private val chaptersOutput = HashMap<MangaChapter, ZipOutput>()
	private val index = MangaIndex(File(rootFile, ENTRY_NAME_INDEX).takeIfReadable()?.readText())

	init {
		index.setMangaInfo(manga, append = true)
	}

	override suspend fun mergeWithExisting() = Unit

	override suspend fun addCover(file: File, ext: String) {
		val name = buildString {
			append("cover")
			if (ext.isNotEmpty() && ext.length <= 4) {
				append('.')
				append(ext)
			}
		}
		runInterruptible(Dispatchers.IO) {
			file.copyTo(File(rootFile, name), overwrite = true)
		}
		index.setCoverEntry(name)
		flushIndex()
	}

	override suspend fun addPage(chapter: MangaChapter, file: File, pageNumber: Int, ext: String) {
		val output = chaptersOutput.getOrPut(chapter) {
			ZipOutput(File(rootFile, chapterFileName(chapter) + SUFFIX_TMP))
		}
		val name = buildString {
			append(FILENAME_PATTERN.format(chapter.branch.hashCode(), chapter.number, pageNumber))
			if (ext.isNotEmpty() && ext.length <= 4) {
				append('.')
				append(ext)
			}
		}
		runInterruptible(Dispatchers.IO) {
			output.put(name, file)
		}
		index.addChapter(chapter, chapterFileName(chapter))
	}

	override suspend fun flushChapter(chapter: MangaChapter): Boolean {
		val output = chaptersOutput.remove(chapter) ?: return false
		output.flushAndFinish()
		flushIndex()
		return true
	}

	override suspend fun finish() {
		flushIndex()
		for (output in chaptersOutput.values) {
			output.flushAndFinish()
		}
		chaptersOutput.clear()
	}

	override suspend fun cleanup() {
		for (output in chaptersOutput.values) {
			output.file.deleteAwait()
		}
	}

	override fun close() {
		for (output in chaptersOutput.values) {
			output.close()
		}
	}

	suspend fun deleteChapter(chapterId: Long) {
		val chapter = checkNotNull(index.getMangaInfo()?.chapters) {
			"No chapters found"
		}.findById(chapterId) ?: error("Chapter not found")
		val chapterDir = File(rootFile, chapterFileName(chapter))
		chapterDir.deleteAwait()
		index.removeChapter(chapterId)
	}

	fun setIndex(newIndex: MangaIndex) {
		index.setFrom(newIndex)
	}

	private suspend fun ZipOutput.flushAndFinish() = runInterruptible(Dispatchers.IO) {
		finish()
		close()
		val resFile = File(file.absolutePath.removeSuffix(SUFFIX_TMP))
		file.renameTo(resFile)
	}

	private fun chapterFileName(chapter: MangaChapter): String {
		index.getChapterFileName(chapter.id)?.let {
			return it
		}
		val baseName = "${chapter.number}_${chapter.name.toFileNameSafe()}".take(18)
		var i = 0
		while (true) {
			val name = (if (i == 0) baseName else baseName + "_$i") + ".cbz"
			if (!File(rootFile, name).exists()) {
				return name
			}
			i++
		}
	}

	private suspend fun flushIndex() = runInterruptible(Dispatchers.IO) {
		File(rootFile, ENTRY_NAME_INDEX).writeText(index.toString())
	}

	companion object {

		private const val FILENAME_PATTERN = "%08d_%03d%03d"
	}
}
