package org.koitharu.kotatsu.reader.domain

import android.util.LongSparseArray
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import javax.inject.Inject

private const val PAGES_TRIM_THRESHOLD = 120

data class ChapterNavigationEvent(
	val wasFallback: Boolean,
	val message: String?,
	val hasChapterGap: Boolean = false,
	val previousChapterNumber: Float? = null,
	val nextChapterNumber: Float? = null,
	val newBranch: String? = null,  // Target branch for fallback
	val oldBranch: String? = null,  // Original branch before fallback
)

@ViewModelScoped
class ChaptersLoader @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val translationFallbackManager: TranslationFallbackManager,
) {

	private val chapters = LongSparseArray<MangaChapter>()
	private val chapterPages = ChapterPages()
	private val mutex = Mutex()
	private var currentManga: MangaDetails? = null
	private var navigationEventCallback: ((ChapterNavigationEvent) -> Unit)? = null
	private var isInitialized = false

	val size: Int
		get() = chapters.size()

	fun setNavigationEventCallback(callback: (ChapterNavigationEvent) -> Unit) {
		navigationEventCallback = callback
	}

	fun clearNavigationEventCallback() {
		navigationEventCallback = null
	}

	suspend fun init(manga: MangaDetails) = mutex.withLock {
		// Always update with the latest manga data
		// This ensures we get new chapters if they've been added from different branches
		currentManga = manga
		
		// Add new chapters to the cache without clearing existing ones
		// This allows us to accumulate chapters from different branches
		manga.allChapters.forEach {
			chapters.put(it.id, it)
		}
		isInitialized = true
	}

	suspend fun loadPrevNextChapter(manga: MangaDetails, currentId: Long, isNext: Boolean) {
		// Use the complete manga data stored during initialization instead of the filtered parameter
		val completeManga = currentManga ?: manga
		val chapters = completeManga.allChapters
		
		val predicate: (MangaChapter) -> Boolean = { it.id == currentId }
		val index = if (isNext) chapters.indexOfFirst(predicate) else chapters.indexOfLast(predicate)
		if (index == -1) return
		
		// Get current chapter info for fallback logic
		val currentChapter = chapters.find { it.id == currentId }
		val currentBranch = currentChapter?.branch
		
		// First, try to find next/prev chapter in the same branch
		val targetIndex = if (isNext) index + 1 else index - 1
		val targetChapter = chapters.getOrNull(targetIndex)
		
		val newChapter = if (targetChapter != null && targetChapter.branch == currentBranch) {
			// Found chapter in same branch, use it
			targetChapter
		} else {
			// No chapter in same branch, try translation fallback
			try {
				val direction = if (isNext) 1 else -1
				val fallbackResult = translationFallbackManager.findBestAvailableBranch(
					manga = completeManga.toManga(),
					currentChapterId = currentId,
					direction = direction,
					history = null // We don't have history context here
				)
				
				if (fallbackResult.branch != null && fallbackResult.chapterId != null) {
					// Use the specific chapter ID from the fallback result
					val fallbackChapter = completeManga.allChapters.find { it.id == fallbackResult.chapterId }
					
					// Emit navigation event for UI notifications including branch change
					if (fallbackResult.wasFallback || fallbackResult.hasChapterGap) {
						try {
							val event = ChapterNavigationEvent(
								wasFallback = fallbackResult.wasFallback,
								message = fallbackResult.fallbackReason,
								hasChapterGap = fallbackResult.hasChapterGap,
								previousChapterNumber = fallbackResult.previousChapterNumber,
								nextChapterNumber = fallbackResult.nextChapterNumber,
								newBranch = fallbackResult.branch,
								oldBranch = currentBranch
							)
							navigationEventCallback?.invoke(event)
						} catch (e: Exception) {
							// Ignore callback errors - don't let UI issues break navigation
						}
					}
					
					fallbackChapter
				} else {
					// Emit "no more chapters" event
					try {
						val event = ChapterNavigationEvent(
							wasFallback = false,
							message = "No more chapters available",
							newBranch = null,
							oldBranch = currentBranch
						)
						navigationEventCallback?.invoke(event)
					} catch (e: Exception) {
						// Ignore callback errors
					}
					
					null
				}
			} catch (e: Exception) {
				// Fallback logic failed, emit error event and return null
				try {
					val event = ChapterNavigationEvent(
						wasFallback = false,
						message = "Translation fallback unavailable",
						newBranch = null,
						oldBranch = currentBranch
					)
					navigationEventCallback?.invoke(event)
				} catch (callbackError: Exception) {
					// Ignore callback errors
				}
				null
			}
		}
		
		if (newChapter == null) return
		
		val newPages = loadChapter(newChapter.id)
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

	suspend fun loadSingleChapter(chapterId: Long) {
		val pages = loadChapter(chapterId)
		mutex.withLock {
			chapterPages.clear()
			chapterPages.addLast(chapterId, pages)
		}
	}

	fun peekChapter(chapterId: Long): MangaChapter? = chapters[chapterId]

	fun hasPages(chapterId: Long): Boolean {
		return chapterId in chapterPages
	}

	fun getPages(chapterId: Long): List<MangaPage> = synchronized(chapterPages) {
		return chapterPages.subList(chapterId).map { it.toMangaPage() }
	}

	fun getPagesCount(chapterId: Long): Int {
		return chapterPages.size(chapterId)
	}

	fun last() = chapterPages.last()

	fun first() = chapterPages.first()
	
	private fun findBestChapterInBranch(
		allChapters: List<MangaChapter>,
		currentChapterId: Long,
		targetBranch: String?,
		direction: Int
	): MangaChapter? {
		if (targetBranch == null) return null
		
		val currentChapter = allChapters.findById(currentChapterId)
		val branchChapters = allChapters.filter { it.branch == targetBranch }.sortedBy { it.number }
		
		if (currentChapter == null || branchChapters.isEmpty()) {
			return null
		}
		
		// For forward direction, try to find a chapter with similar or next number
		return if (direction > 0) {
			// Find first chapter in target branch with number >= current chapter number
			branchChapters.find { it.number >= currentChapter.number }
				?: branchChapters.firstOrNull() // Fallback to first chapter in branch
		} else {
			// For backward direction, find last chapter with number <= current chapter number
			branchChapters.findLast { it.number <= currentChapter.number }
				?: branchChapters.lastOrNull() // Fallback to last chapter in branch
		}
	}

	fun snapshot() = chapterPages.toList()

	/**
	 * Get all chapters for a specific branch from the complete manga data
	 */
	fun getChaptersByBranch(branch: String?): List<MangaChapter>? {
		return currentManga?.chapters?.get(branch)?.sortedBy { it.number }
	}

	/**
	 * Get the count of chapters in a specific branch
	 */
	fun getChaptersCount(branch: String?): Int {
		return currentManga?.chapters?.get(branch).sizeOrZero()
	}

	/**
	 * Get all available branches from the complete manga data
	 */
	fun getAllBranches(): Set<String?> {
		return currentManga?.chapters?.keys.orEmpty()
	}

	/**
	 * Find the index of a specific chapter within its branch
	 */
	fun getChapterIndexInBranch(chapterId: Long): Pair<Int, Int>? {
		val chapter = chapters[chapterId] ?: return null
		val branchChapters = getChaptersByBranch(chapter.branch) ?: return null
		val index = branchChapters.indexOfFirst { it.id == chapterId }
		return if (index >= 0) index to branchChapters.size else null
	}

	private suspend fun loadChapter(chapterId: Long): List<ReaderPage> {
		val chapter = chapters[chapterId] 
			?: throw IllegalStateException("Requested chapter not found: $chapterId. Available chapters: ${chapters.size()}")
		val repo = mangaRepositoryFactory.create(chapter.source)
		return repo.getPages(chapter).mapIndexed { index, page ->
			ReaderPage(page, index, chapterId)
		}
	}
}
