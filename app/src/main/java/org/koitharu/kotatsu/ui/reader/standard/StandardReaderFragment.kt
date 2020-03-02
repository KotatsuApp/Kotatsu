package org.koitharu.kotatsu.ui.reader.standard

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_reader_standard.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.*
import org.koitharu.kotatsu.utils.ext.callOnPageChaneListeners
import org.koitharu.kotatsu.utils.ext.doOnPageChanged

class StandardReaderFragment : BaseReaderFragment(R.layout.fragment_reader_standard),
	OnBoundsScrollListener {

	private val presenter by moxyPresenter(factory = ReaderPresenter.Companion::getInstance)

	private var adapter: PagesAdapter? = null
	private lateinit var loader: PageLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		loader = PageLoader()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = PagesAdapter(loader)
		pager.adapter = adapter
		pager.offscreenPageLimit = 2
		pager.registerOnPageChangeCallback(PagerPaginationListener(adapter!!, 2, this))
		pager.doOnPageChanged(::notifyPageChanged)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	override fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>, action: ReaderAction) {
		super.onPagesLoaded(chapterId, pages, action)
		when (action) {
			ReaderAction.REPLACE -> adapter?.let {
				it.replaceData(pages)
				lastState?.let { state ->
					if (chapterId == state.chapterId) {
						pager.setCurrentItem(findChapterOffset(chapterId) + state.page, false)
					}
				}
			}
			ReaderAction.PREPEND -> adapter?.run {
				val pos = pager.currentItem
				prependData(pages)
				pager.setCurrentItem(pos + pages.size, false)
			}
			ReaderAction.APPEND -> adapter?.appendData(pages)
		}
		pager.callOnPageChaneListeners()
	}

	override fun onDestroy() {
		loader.dispose()
		super.onDestroy()
	}

	override fun onScrolledToStart() {
		val prevChapterId = getPrevChapterId()
		if (prevChapterId != 0L) {
			presenter.loadChapter(lastState?.manga ?: return, prevChapterId, ReaderAction.PREPEND)
		}
	}

	override fun onScrolledToEnd() {
		val nextChapterId = getNextChapterId()
		if (nextChapterId != 0L) {
			presenter.loadChapter(lastState?.manga ?: return, nextChapterId, ReaderAction.APPEND)
		}
	}

	override val hasItems: Boolean
		get() = adapter?.hasItems == true

	override val currentPageIndex: Int
		get() = pager.currentItem

	override val pages: List<MangaPage>
		get() = adapter?.items.orEmpty()

	override fun setCurrentPage(index: Int, smooth: Boolean) {
		pager.setCurrentItem(index, smooth)
	}

	private companion object {

		const val SCROLL_OFFSET = 2
	}
}