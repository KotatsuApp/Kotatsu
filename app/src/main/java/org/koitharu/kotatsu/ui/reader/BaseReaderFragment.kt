package org.koitharu.kotatsu.ui.reader

import android.net.Uri
import androidx.annotation.CallSuper
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

	@CallSuper
	override fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>, action: ReaderAction) {
		when (action) {
			ReaderAction.REPLACE -> {
				chaptersMap.clear()
				chaptersMap.add(chapterId to pages.size)
			}
			ReaderAction.PREPEND -> chaptersMap.addFirst(chapterId to pages.size)
			ReaderAction.APPEND -> chaptersMap.addLast(chapterId to pages.size)
		}
	}

	fun switchPageBy(delta: Int) {
		setCurrentPage(currentPageIndex + delta, true)
	}

	fun findCurrentPageIndex(chapterId: Long): Int {
		var offset = 0
		for ((id, count) in chaptersMap) {
			if (id == chapterId) {
				return currentPageIndex - offset
			}
			offset += count
		}
		return -1
	}

	fun findChapterOffset(chapterId: Long): Int {
		var offset = 0
		for ((id, count) in chaptersMap) {
			if (id == chapterId) {
				return offset
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
			i < 0
		} ?: return
		(activity as? ReaderListener)?.onPageChanged(
			chapter = chapters.find { x -> x.id == chapter.first } ?: return,
			page = i + chapter.second,
			total = chapter.second
		)
	}
}