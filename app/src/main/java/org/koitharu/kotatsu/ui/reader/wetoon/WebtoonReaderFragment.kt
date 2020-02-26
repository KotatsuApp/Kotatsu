package org.koitharu.kotatsu.ui.reader.wetoon

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_reader_webtoon.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.BaseReaderFragment
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.ReaderPresenter
import org.koitharu.kotatsu.utils.ext.firstItem

class WebtoonReaderFragment : BaseReaderFragment(R.layout.fragment_reader_webtoon) {

	private val presenter by moxyPresenter(factory = ReaderPresenter.Companion::getInstance)

	private var adapter: WebtoonAdapter? = null
	private lateinit var loader: PageLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		loader = PageLoader()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = WebtoonAdapter(loader)
		recyclerView.adapter = adapter
	}

	override fun onPagesLoaded(chapterId: Long, pages: List<MangaPage>) {
		adapter?.let {
			it.replaceData(pages)
			lastState?.let { state ->
				if (chapterId == state.chapterId) {
					recyclerView.firstItem = state.page
				}
			}
		}
	}

	override fun onDestroy() {
		loader.dispose()
		super.onDestroy()
	}

	override val hasItems: Boolean
		get() = adapter?.hasItems == true

	override val currentPageIndex: Int
		get() = recyclerView.firstItem

	override val pages: List<MangaPage>
		get() = adapter?.items.orEmpty()

	override fun setCurrentPage(index: Int, smooth: Boolean) {
		if (smooth) {
			recyclerView.smoothScrollToPosition(index)
		} else {
			recyclerView.firstItem = index
		}
	}
}