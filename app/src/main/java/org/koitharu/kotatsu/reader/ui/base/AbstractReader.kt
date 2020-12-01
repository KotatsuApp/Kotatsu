package org.koitharu.kotatsu.reader.ui.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.collection.LongSparseArray
import androidx.core.view.postDelayed
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderListener
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.ext.associateByLong
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

abstract class AbstractReader<B : ViewBinding> : BaseFragment<B>(), OnBoundsScrollListener {

	protected lateinit var manga: Manga
		private set
	private lateinit var chapters: LongSparseArray<MangaChapter>
	protected val loader by lazy(LazyThreadSafetyMode.NONE) {
		PageLoader()
	}
	protected val pages = ArrayDeque<ReaderPage>()
	protected var readerAdapter: BaseReaderAdapter? = null
		private set

	val itemsCount: Int
		get() = readerAdapter?.itemCount ?: 0

	val hasItems: Boolean
		get() = itemsCount != 0

	val currentPage: MangaPage?
		get() = pages.getOrNull(getCurrentItem())?.toMangaPage()

	private var readerListener: ReaderListener? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		manga = requireNotNull(requireArguments().getParcelable<ReaderState>(ARG_STATE)).manga
		chapters = requireNotNull(manga.chapters).associateByLong { it.id }
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		readerAdapter = onCreateAdapter(pages)
		@Suppress("RemoveExplicitTypeArguments")
		val state = savedInstanceState?.getParcelable<ReaderState>(ARG_STATE)
			?: requireArguments().getParcelable<ReaderState>(ARG_STATE)!!
		loadChapter(state.chapterId) {
			pages.clear()
			it.mapIndexedTo(pages) { i, p ->
				ReaderPage.from(p, i, state.chapterId)
			}
			readerAdapter?.notifyDataSetChanged()
			setCurrentItem(state.page, false)
			if (state.scroll != 0) {
				restorePageScroll(state.page, state.scroll)
			}
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		readerListener = activity as? ReaderListener
	}

	override fun onDetach() {
		readerListener = null
		super.onDetach()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val page = pages.getOrNull(getCurrentItem()) ?: return
		outState.putParcelable(
			ARG_STATE, ReaderState(
				manga = manga,
				chapterId = page.chapterId,
				page = page.index,
				scroll = getCurrentPageScroll()
			)
		)
	}

	override fun onScrolledToStart() {
		val chapterId = getFirstPage()?.chapterId ?: return
		val index = manga.chapters?.indexOfFirst { it.id == chapterId } ?: return
		val prevChapterId = manga.chapters!!.getOrNull(index - 1)?.id ?: return
		loadChapter(prevChapterId) {
			pages.addAll(0, it.mapIndexed { i, p ->
				ReaderPage.from(p, i, prevChapterId)
			})
			readerAdapter?.notifyItemsPrepended(it.size)
			view?.postDelayed(500) {
				trimEnd()
			}
		}
	}

	override fun onScrolledToEnd() {
		val chapterId = getLastPage()?.chapterId ?: return
		val index = manga.chapters?.indexOfLast { it.id == chapterId } ?: return
		val nextChapterId = manga.chapters!!.getOrNull(index + 1)?.id ?: return
		loadChapter(nextChapterId) {
			pages.addAll(it.mapIndexed { i, p ->
				ReaderPage.from(p, i, nextChapterId)
			})
			readerAdapter?.notifyItemsAppended(it.size)
			view?.postDelayed(500) {
				trimStart()
			}
		}
	}

	override fun onDestroyView() {
		readerAdapter = null
		super.onDestroyView()
	}

	override fun onDestroy() {
		loader.dispose()
		super.onDestroy()
	}

	@CallSuper
	open fun recreateAdapter() {
		readerAdapter = onCreateAdapter(pages)
	}

	fun getPages(): List<MangaPage>? {
		val chapterId = (pages.getOrNull(getCurrentItem()) ?: return null).chapterId
		// TODO optimize
		return pages.filter { it.chapterId == chapterId }.map { it.toMangaPage() }
	}

	override fun onPause() {
		saveState()
		super.onPause()
	}

	private fun loadChapter(chapterId: Long, callback: suspend (List<MangaPage>) -> Unit) {
		viewLifecycleScope.launch {
			readerListener?.onLoadingStateChanged(isLoading = true)
			try {
				val pages = withContext(Dispatchers.Default) {
					val chapter = chapters.get(chapterId)
						?: throw RuntimeException("Chapter $chapterId not found")
					val repo = manga.source.repository
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
		/*var removed = 0
		while (pages.groupCount > 3 && pages.size > 8) {
			removed += pages.removeFirst().size
		}
		if (removed != 0) {
			adapter?.notifyItemsRemovedStart(removed)
			Log.i(TAG, "Removed $removed pages from start")
		}*/
	}

	private fun trimEnd() {
		/*var removed = 0
		while (pages.groupCount > 3 && pages.size > 8) {
			removed += pages.removeLast().size
		}
		if (removed != 0) {
			adapter?.notifyItemsRemovedEnd(removed)
			Log.i(TAG, "Removed $removed pages from end")
		}*/
	}

	protected fun notifyPageChanged(position: Int) {
		val page = pages.getOrNull(position) ?: return
		val chapter = chapters.get(page.chapterId) ?: return
		readerListener?.onPageChanged(
			chapter = chapter,
			page = page.index
		)
	}

	private fun saveState() {
		val page = pages.getOrNull(getCurrentItem()) ?: return
		readerListener?.saveState(page.chapterId, page.index, getCurrentPageScroll())
	}

	open fun switchPageBy(delta: Int) {
		setCurrentItem(getCurrentItem() + delta, true)
	}

	fun updateState(chapterId: Long = 0, pageId: Long = 0) {
		val currentChapterId = pages.getOrNull(getCurrentItem())?.chapterId ?: 0L
		if (chapterId != 0L && chapterId != currentChapterId) {
			pages.clear()
			readerAdapter?.notifyDataSetChanged()
			loadChapter(chapterId) {
				pages.clear()
				it.mapIndexedTo(pages) { i, p ->
					ReaderPage.from(p, i, chapterId)
				}
				readerAdapter?.notifyDataSetChanged()
				setCurrentItem(
					if (pageId == 0L) {
						0
					} else {
						it.indexOfFirst { x -> x.id == pageId }.coerceAtLeast(0)
					}, false
				)
			}
		} else {
			var index = 0
			if (pageId != 0L) {
				index = pages.indexOfFirst {
					it.chapterId == currentChapterId && it.id == pageId
				}
				if (index == -1) { // try to find chapter at least
					index = pages.indexOfFirst {
						it.chapterId == currentChapterId
					}
				}
				if (index == -1) {
					index = 0
				}
			}
			setCurrentItem(index, false)
		}
	}

	protected open fun getLastPage() = pages.lastOrNull()

	protected open fun getFirstPage() = pages.firstOrNull()

	protected abstract fun getCurrentItem(): Int

	protected abstract fun getCurrentPageScroll(): Int

	protected abstract fun restorePageScroll(position: Int, scroll: Int)

	protected abstract fun setCurrentItem(position: Int, isSmooth: Boolean)

	protected abstract fun onCreateAdapter(dataSet: List<ReaderPage>): BaseReaderAdapter

	protected companion object {

		const val ARG_STATE = "state"
	}
}