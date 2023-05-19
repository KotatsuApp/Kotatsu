package org.koitharu.kotatsu.local.data.output

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.kotatsu.core.util.ext.deleteAwait
import org.koitharu.kotatsu.core.util.ext.readText
import org.koitharu.kotatsu.core.zip.ZipOutput
import org.koitharu.kotatsu.local.data.MangaIndex
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import java.io.File
import java.util.zip.ZipFile

class LocalMangaZipOutput(
	rootFile: File,
	manga: Manga,
) : LocalMangaOutput(rootFile) {

	private val output = ZipOutput(File(rootFile.path + ".tmp"))
	private val index = MangaIndex(null)

	init {
		index.setMangaInfo(manga, false)
	}

	override suspend fun mergeWithExisting() {
		if (rootFile.exists()) {
			runInterruptible(Dispatchers.IO) {
				mergeWith(rootFile)
			}
		}
	}

	override suspend fun addCover(file: File, ext: String) {
		val name = buildString {
			append(FILENAME_PATTERN.format(0, 0, 0))
			if (ext.isNotEmpty() && ext.length <= 4) {
				append('.')
				append(ext)
			}
		}
		runInterruptible(Dispatchers.IO) {
			output.put(name, file)
		}
		index.setCoverEntry(name)
	}

	override suspend fun addPage(chapter: MangaChapter, file: File, pageNumber: Int, ext: String) {
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
		index.addChapter(chapter, null)
	}

	override suspend fun flushChapter(chapter: MangaChapter): Boolean = false

	override suspend fun finish() {
		runInterruptible(Dispatchers.IO) {
			output.put(ENTRY_NAME_INDEX, index.toString())
			output.finish()
			output.close()
		}
		rootFile.deleteAwait()
		output.file.renameTo(rootFile)
	}

	override suspend fun cleanup() {
		output.file.deleteAwait()
	}

	override fun close() {
		output.close()
	}

	@WorkerThread
	private fun mergeWith(other: File) {
		var otherIndex: MangaIndex? = null
		ZipFile(other).use { zip ->
			for (entry in zip.entries()) {
				if (entry.name == ENTRY_NAME_INDEX) {
					otherIndex = MangaIndex(
						zip.getInputStream(entry).use {
							it.reader().readText()
						},
					)
				} else {
					output.copyEntryFrom(zip, entry)
				}
			}
		}
		otherIndex?.getMangaInfo()?.chapters?.let { chapters ->
			for (chapter in chapters) {
				index.addChapter(chapter, null)
			}
		}
	}

	companion object {

		private const val FILENAME_PATTERN = "%08d_%03d%03d"

		@WorkerThread
		fun filterChapters(subject: LocalMangaZipOutput, idsToRemove: Set<Long>) {
			ZipFile(subject.rootFile).use { zip ->
				val index = MangaIndex(zip.readText(zip.getEntry(ENTRY_NAME_INDEX)))
				idsToRemove.forEach { id -> index.removeChapter(id) }
				val patterns = requireNotNull(index.getMangaInfo()?.chapters).map {
					index.getChapterNamesPattern(it)
				}
				val coverEntryName = index.getCoverEntry()
				for (entry in zip.entries()) {
					when {
						entry.name == ENTRY_NAME_INDEX -> {
							subject.output.put(ENTRY_NAME_INDEX, index.toString())
						}

						entry.isDirectory -> {
							subject.output.addDirectory(entry.name)
						}

						entry.name == coverEntryName -> {
							subject.output.copyEntryFrom(zip, entry)
						}

						else -> {
							val name = entry.name.substringBefore('.')
							if (patterns.any { it.matches(name) }) {
								subject.output.copyEntryFrom(zip, entry)
							}
						}
					}
				}
				subject.output.finish()
				subject.output.close()
				subject.rootFile.delete()
				subject.output.file.renameTo(subject.rootFile)
			}
		}
	}
}
