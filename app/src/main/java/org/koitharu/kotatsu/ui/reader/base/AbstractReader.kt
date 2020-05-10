package org.koitharu.kotatsu.ui.reader.base

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.postDelayed
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BaseFragment
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.ReaderListener
import org.koitharu.kotatsu.ui.reader.ReaderState

abstract class AbstractReader(contentLayoutId: Int) : BaseFragment(contentLayoutId),
	OnBoundsScrollListener {

	protected lateinit var manga: Manga
	protected lateinit var loader: PageLoader
		private set
	private lateinit var pages: GroupedList<Long, MangaPage>
	protected var adapter: BaseReaderAdapter? = null
		private set

	val hasItems: Boolean
		get() = itemsCount != 0

	val currentPage: MangaPage?
		get() = pages.getOrNull(getCurrentItem())

	protected val readerListener: ReaderListener?
		get() = activity as? ReaderListener

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		pages = GroupedList()
		manga = requireArguments().getParcelable<ReaderState>(ARG_STATE)!!.manga
		loader = PageLoader()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = onCreateAdapter(pages)
		@Suppress("RemoveExplicitTypeArguments")
		val state = savedInstanceState?.getParcelable<ReaderState>(ARG_STATE)
			?: requireArguments().getParcelable<ReaderState>(ARG_STATE)!!
		loadChapter(state.chapterId) {
			pages.clear()
			pages.addLast(state.chapterId, it)
			adapter?.notifyDataSetChanged()
			setCurrentItem(state.page, false)
			if (state.scroll != 0f) {
				restorePageScroll(state.page, state.scroll)
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putParcelable(
			ARG_STATE, ReaderState(
				manga = manga,
				chapterId = pages.findGroupByIndex(getCurrentItem()) ?: return,
				page = pages.getRelativeIndex(getCurrentItem()),
				scroll = getCurrentPageScroll()
			)
		)
	}

	override fun onScrolledToStart() {
		val chapterId = pages.findGroupByIndex(getCurrentItem()) ?: return
		val index = manga.chapters?.indexOfFirst { it.id == chapterId } ?: return
		val prevChapterId = manga.chapters!!.getOrNull(index - 1)?.id ?: return
		loadChapter(prevChapterId) {
			pages.addFirst(prevChapterId, it)
			adapter?.notifyItemsPrepended(it.size)
			view?.postDelayed(500) {
				trimEnd()
			}
		}
	}

	override fun onScrolledToEnd() {
		val chapterId = pages.findGroupByIndex(getCurrentItem()) ?: return
		val index = manga.chapters?.indexOfFirst { it.id == chapterId } ?: return
		val nextChapterId = manga.chapters!!.getOrNull(index + 1)?.id ?: return
		loadChapter(nextChapterId) {
			pages.addLast(nextChapterId, it)
			adapter?.notifyItemsAppended(it.size)
			view?.postDelayed(500) {
				trimStart()
			}
		}
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onDestroy() {
		loader.dispose()
		super.onDestroy()
	}

	fun getPages() = pages.findGroupByIndex(getCurrentItem())?.let {
		pages.getGroup(it)
	}

	override fun onPause() {
		saveState()
		super.onPause()
	}

	private fun loadChapter(chapterId: Long, callback: suspend (List<MangaPage>) -> Unit) {
		lifecycleScope.launch {
			readerListener?.onLoadingStateChanged(isLoading = true)
			try {
				val pages = withContext(Dispatchers.IO) {
					val chapter = manga.chapters?.find { it.id == chapterId }
						?: throw RuntimeException("Chapter $chapterId not found")
					val repo = MangaProviderFactory.create(manga.source)
					repo.getPages(chapter)
				}
				callback(pages)
			} catch (_: CancellationException) {
			} catch (e: Throwable) {
				readerListener?.onError(e)
			} finally {
				readerListener?.onLoadingStateChanged(isLoading = false)
			}
		}
	}

	private fun trimStart() {
		var removed = 0
		while (pages.groupCount > 3 && pages.size > 8) {
			removed += pages.removeFirst().size
		}
		if (removed != 0) {
			adapter?.notifyItemsRemovedStart(removed)
			Log.i(TAG, "Removed $removed pages from start")
		}
	}

	private fun trimEnd() {
		var removed = 0
		while (pages.groupCount > 3 && pages.size > 8) {
			removed += pages.removeLast().size
		}
		if (removed != 0) {
			adapter?.notifyItemsRemovedEnd(removed)
			Log.i(TAG, "Removed $removed pages from end")
		}
	}

	protected fun notifyPageChanged(page: Int) {
		val chapters = manga.chapters ?: return
		val chapterId = pages.findGroupByIndex(page) ?: return
		val chapter = chapters.find { it.id == chapterId } ?: return
		readerListener?.onPageChanged(
			chapter = chapter,
			page = page - pages.getGroupOffset(chapterId),
			total = pages.getGroup(chapterId)?.size ?: return
		)
	}

	protected fun saveState() {
		val chapterId = pages.findGroupByIndex(getCurrentItem()) ?: return
		val page = pages.getRelativeIndex(getCurrentItem())
		if (page != -1) {
			readerListener?.saveState(chapterId, page, getCurrentPageScroll())
		}
		Log.i(TAG, "saveState(chapterId=$chapterId, page=$page)")
	}

	open fun switchPageBy(delta: Int) {
		setCurrentItem(getCurrentItem() + delta, true)
	}

	fun updateState(chapterId: Long = 0, pageId: Long = 0) {
		val currentChapterId = pages.findGroupByIndex(getCurrentItem())
		if (chapterId != 0L && chapterId != currentChapterId) {
			pages.clear()
			adapter?.notifyDataSetChanged()
			loadChapter(chapterId) {
				pages.clear()
				pages.addLast(chapterId, it)
				adapter?.notifyDataSetChanged()
				setCurrentItem(
					if (pageId == 0L) {
						0
					} else {
						it.indexOfFirst { it.id == pageId }.coerceAtLeast(0)
					}, false
				)
			}
		} else {
			setCurrentItem(
				if (pageId == 0L) {
					0
				} else {
					val chapterPages = pages.getGroup(currentChapterId ?: return) ?: return
					chapterPages.indexOfFirst { it.id == pageId }
						.coerceAtLeast(0) + pages.getGroupOffset(currentChapterId)
				}, false
			)
		}
	}

	abstract val itemsCount: Int

	protected abstract fun getCurrentItem(): Int

	protected abstract fun getCurrentPageScroll(): Float

	protected abstract fun restorePageScroll(position: Int, scroll: Float)

	protected abstract fun setCurrentItem(position: Int, isSmooth: Boolean)

	protected abstract fun onCreateAdapter(dataSet: GroupedList<Long, MangaPage>): BaseReaderAdapter

	protected companion object {

		const val ARG_STATE = "state"
		private const val TAG = "AbstractReader"

	}
}