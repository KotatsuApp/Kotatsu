package org.koitharu.kotatsu.local.data.output

import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.util.MimeTypes
import org.koitharu.kotatsu.core.util.ext.MimeType
import org.koitharu.kotatsu.core.util.ext.deleteAwait
import org.koitharu.kotatsu.core.util.ext.takeIfReadable
import org.koitharu.kotatsu.core.util.ext.toFileNameSafe
import org.koitharu.kotatsu.core.zip.ZipOutput
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.local.data.input.LocalMangaParser
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
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

	override suspend fun addCover(file: File, type: MimeType?) = mutex.withLock {
		val name = buildString {
			append("cover")
			MimeTypes.getExtension(type)?.let { ext ->
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

	override suspend fun addPage(chapter: IndexedValue<MangaChapter>, file: File, pageNumber: Int, type: MimeType?) =
		mutex.withLock {
			val output = chaptersOutput.getOrPut(chapter.value) {
				ZipOutput(File(rootFile, chapterFileName(chapter) + SUFFIX_TMP))
			}
			val name = buildString {
				append(FILENAME_PATTERN.format(chapter.value.branch.hashCode(), chapter.index + 1, pageNumber))
				MimeTypes.getExtension(type)?.let { ext ->
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
			output.closeQuietly()
		}
	}

	suspend fun deleteChapters(ids: Set<Long>) = mutex.withLock {
		val chapters = checkNotNull(
			(index.getMangaInfo() ?: LocalMangaParser(rootFile).getManga(withDetails = true).manga).chapters,
		) {
			"No chapters found"
		}.withIndex()
		val victimsIds = ids.toMutableSet()
		for (chapter in chapters) {
			if (!victimsIds.remove(chapter.value.id)) {
				continue
			}
			val chapterFile = index.getChapterFileName(chapter.value.id)?.let {
				File(rootFile, it)
			} ?: chapter.value.url.toUri().toFile()
			chapterFile.deleteAwait()
			index.removeChapter(chapter.value.id)
		}
		check(victimsIds.isEmpty()) {
			"${victimsIds.size} of ${ids.size} chapters was not removed: not found"
		}
	}

	fun setIndex(newIndex: MangaIndex) {
		index.setFrom(newIndex)
	}

	private suspend fun ZipOutput.flushAndFinish() = runInterruptible(Dispatchers.IO) {
		val e: Throwable? = try {
			finish()
			null
		} catch (e: Throwable) {
			e
		} finally {
			close()
		}
		if (e == null) {
			val resFile = File(file.absolutePath.removeSuffix(SUFFIX_TMP))
			file.renameTo(resFile)
		} else {
			file.delete()
			throw e
		}
	}

	private fun chapterFileName(chapter: IndexedValue<MangaChapter>): String {
		index.getChapterFileName(chapter.value.id)?.let {
			return it
		}
		val baseName = buildString {
			append(chapter.index)
			chapter.value.title?.nullIfEmpty()?.let {
				append('_')
				append(it.toFileNameSafe())
			}
			if (length > 32) {
				deleteRange(31, lastIndex)
			}
		}
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

		private const val FILENAME_PATTERN = "%08d_%04d%04d"
	}
}
