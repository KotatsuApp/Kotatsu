package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import androidx.annotation.LayoutRes
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.ui.common.BaseFragment
import java.util.*

abstract class BaseReaderFragment(@LayoutRes contentLayoutId: Int) : BaseFragment(contentLayoutId),
	ReaderView {

	private val chaptersMap = ArrayDeque<Pair<Long, Int>>() as Deque<Pair<Long, Int>>

	protected val lastState
		get() = (activity as? ReaderActivity)?.state

	abstract val hasItems: Boolean

	protected abstract val currentPageIndex: Int

	abstract val pages: List<MangaPage>

	abstract fun setCurrentPage(index: Int, smooth: Boolean)

	val currentPage get() = pages.getOrNull(currentPageIndex)

	/**
	 * Handled by activity
	 */
	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	/**
	 * Handled by activity
	 */
	override fun onError(e: Exception) = Unit

	/**
	 * Handled by activity
	 */
	override fun onPageSaved(uri: Uri?) = Unit


	override fun onInitReader(mode: ReaderMode) = Unit

	override fun onChaptersLoader(chapters: List<MangaChapter>) = Unit

	override fun onDestroyView() {
		chaptersMap.clear()
		super.onDestroyView()
	}

	final override fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>) {
		when {
			chaptersMap.isEmpty() -> {
				chaptersMap.push(chapterId to pages.size)
				onPagesLoaded(chapterId, pages, Action.REPLACE)
			}
			shouldAppend(chapterId) -> {
				chaptersMap.addLast(chapterId to pages.size)
				onPagesLoaded(chapterId, pages, Action.APPEND)
			}
			shouldPrepend(chapterId) -> {
				chaptersMap.addFirst(chapterId to pages.size)
				onPagesLoaded(chapterId, pages, Action.PREPEND)
			}
			else -> {
				chaptersMap.clear()
				chaptersMap.push(chapterId to pages.size)
				onPagesLoaded(chapterId, pages, Action.REPLACE)
			}
		}
	}

	fun switchPageBy(delta: Int) {
		setCurrentPage(currentPageIndex + delta, true)
	}

	fun findCurrentPageIndex(chapterId: Long): Int {
		val pages = this.pages
		var offset = 0
		for ((id, count) in chaptersMap) {
			if (id == chapterId) {
				return currentPageIndex - offset
			}
			offset += count
		}
		return -1
	}

	fun getPages(chapterId: Long): List<MangaPage>? {
		var offset = 0
		for ((id, count) in chaptersMap) {
			if (id == chapterId) {
				return pages.subList(offset, offset + count - 1)
			}
			offset += count
		}
		return null
	}

	private fun shouldAppend(chapterId: Long): Boolean {
		val chapters = lastState?.manga?.chapters ?: return false
		val lastChapterId = chaptersMap.peekLast()?.first ?: return false
		val indexOfCurrent = chapters.indexOfLast { x -> x.id == lastChapterId }
		val indexOfNext = chapters.indexOfLast { x -> x.id == chapterId }
		return indexOfCurrent != -1 && indexOfNext != -1 && indexOfCurrent + 1 == indexOfNext
	}

	private fun shouldPrepend(chapterId: Long): Boolean {
		val chapters = lastState?.manga?.chapters ?: return false
		val firstChapterId = chaptersMap.peekFirst()?.first ?: return false
		val indexOfCurrent = chapters.indexOfFirst { x -> x.id == firstChapterId }
		val indexOfPrev = chapters.indexOfFirst { x -> x.id == chapterId }
		return indexOfCurrent != -1 && indexOfPrev != -1 && indexOfCurrent + 1 == indexOfPrev
	}

	protected fun getNextChapterId(): Long {
		val lastChapterId = chaptersMap.peekLast()?.first ?: return 0
		val chapters = lastState?.manga?.chapters ?: return 0
		val indexOfCurrent = chapters.indexOfLast { x -> x.id == lastChapterId }
		return if (indexOfCurrent == -1) {
			0
		} else {
			chapters.getOrNull(indexOfCurrent + 1)?.id ?: 0
		}
	}

	protected fun getPrevChapterId(): Long {
		val firstChapterId = chaptersMap.peekFirst()?.first ?: return 0
		val chapters = lastState?.manga?.chapters ?: return 0
		val indexOfCurrent = chapters.indexOfFirst { x -> x.id == firstChapterId }
		return if (indexOfCurrent == -1) {
			0
		} else {
			chapters.getOrNull(indexOfCurrent - 1)?.id ?: 0
		}
	}


	protected fun notifyPageChanged(page: Int) {
		var i = page
		val chapters = lastState?.manga?.chapters ?: return
		val chapter = chaptersMap.firstOrNull { x ->
			i -= x.second
			i <= 0
		} ?: return
		(activity as? ReaderListener)?.onPageChanged(
			chapter = chapters.find { x -> x.id == chapter.first } ?: return,
			page = i + chapter.second,
			total = chapter.second
		)
	}

	protected abstract fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>, action: Action)

	protected enum class Action {
		REPLACE, PREPEND, APPEND
	}
}