package org.koitharu.kotatsu.local.data.output

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.core.model.isLocal
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
	private val mutex = Mutex()

	init {
		if (!manga.isLocal) {
			index.setMangaInfo(manga)
		}
	}

	override suspend fun mergeWithExisting() = Unit

	override suspend fun addCover(file: File, ext: String) = mutex.withLock {
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

	override suspend fun addPage(chapter: IndexedValue<MangaChapter>, file: File, pageNumber: Int, ext: String) = mutex.withLock {
		val output = chaptersOutput.getOrPut(chapter.value) {
			ZipOutput(File(rootFile, chapterFileName(chapter) + SUFFIX_TMP))
		}
		val name = buildString {
			append(FILENAME_PATTERN.format(chapter.value.branch.hashCode(), chapter.index + 1, pageNumber))
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

	override suspend fun flushChapter(chapter: MangaChapter): Boolean = mutex.withLock {
		val output = chaptersOutput.remove(chapter) ?: return@withLock false
		output.flushAndFinish()
		flushIndex()
		true
	}

	override suspend fun finish() = mutex.withLock {
		flushIndex()
		for (output in chaptersOutput.values) {
			output.flushAndFinish()
		}
		chaptersOutput.clear()
	}

	override suspend fun cleanup() = mutex.withLock {
		for (output in chaptersOutput.values) {
			output.file.deleteAwait()
		}
	}

	override fun close() {
		for (output in chaptersOutput.values) {
			output.close()
		}
	}

	suspend fun deleteChapter(chapterId: Long) = mutex.withLock {
		val chapter = checkNotNull(index.getMangaInfo()?.chapters?.withIndex()) {
			"No chapters found"
		}.find { x -> x.value.id == chapterId } ?: error("Chapter not found")
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

	private fun chapterFileName(chapter: IndexedValue<MangaChapter>): String {
		index.getChapterFileName(chapter.value.id)?.let {
			return it
		}
		val baseName = "${chapter.index}_${chapter.value.name.toFileNameSafe()}".take(18)
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
