package org.koitharu.kotatsu.reader.domain

import android.util.LongSparseArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

private const val PAGES_TRIM_THRESHOLD = 120

class ChaptersLoader {

	val chapters = LongSparseArray<MangaChapter>()
	private val chapterPages = ChapterPages()
	private val mutex = Mutex()

	suspend fun loadPrevNextChapter(manga: Manga, currentId: Long, isNext: Boolean) {
		val chapters = manga.chapters ?: return
		val predicate: (MangaChapter) -> Boolean = { it.id == currentId }
		val index = if (isNext) chapters.indexOfFirst(predicate) else chapters.indexOfLast(predicate)
		if (index == -1) return
		val newChapter = chapters.getOrNull(if (isNext) index + 1 else index - 1) ?: return
		val newPages = loadChapter(manga, newChapter.id)
		mutex.withLock {
			if (chapterPages.chaptersSize > 1) {
				// trim pages
				if (chapterPages.size > PAGES_TRIM_THRESHOLD) {
					if (isNext) {
						chapterPages.removeFirst()
					} else {
						chapterPages.removeLast()
					}
				}
			}
			if (isNext) {
				chapterPages.addLast(newChapter.id, newPages)
			} else {
				chapterPages.addFirst(newChapter.id, newPages)
			}
		}
	}

	suspend fun loadSingleChapter(manga: Manga, chapterId: Long) {
		val pages = loadChapter(manga, chapterId)
		mutex.withLock {
			chapterPages.clear()
			chapterPages.addLast(chapterId, pages)
		}
	}

	fun getPages(chapterId: Long): List<ReaderPage> {
		return chapterPages.subList(chapterId)
	}

	fun getPagesCount(chapterId: Long): Int {
		return chapterPages.size(chapterId)
	}

	fun snapshot() = chapterPages.toList()

	private suspend fun loadChapter(manga: Manga, chapterId: Long): List<ReaderPage> {
		val chapter = checkNotNull(chapters[chapterId]) { "Requested chapter not found" }
		val repo = MangaRepository(manga.source)
		return repo.getPages(chapter).mapIndexed { index, page ->
			ReaderPage(page, index, chapterId)
		}
	}
}
