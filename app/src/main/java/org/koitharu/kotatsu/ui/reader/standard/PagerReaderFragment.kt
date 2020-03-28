package org.koitharu.kotatsu.ui.reader.standard

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_reader_standard.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.ReaderState
import org.koitharu.kotatsu.ui.reader.base.AbstractReader
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.GroupedList
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.withArgs

class PagerReaderFragment : AbstractReader(R.layout.fragment_reader_standard) {

	private var paginationListener: PagerPaginationListener? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		paginationListener = PagerPaginationListener(adapter!!, 2, this)
		pager.adapter = adapter
		pager.offscreenPageLimit = 2
		pager.registerOnPageChangeCallback(paginationListener!!)
		pager.doOnPageChanged(::notifyPageChanged)
	}

	override fun onDestroyView() {
		paginationListener = null
		super.onDestroyView()
	}

	override fun onCreateAdapter(dataSet: GroupedList<Long, MangaPage>): BaseReaderAdapter {
		return PagesAdapter(dataSet, loader)
	}

	override val itemsCount: Int
		get() = adapter?.itemCount ?: 0

	override fun getCurrentItem() = pager.currentItem

	override fun setCurrentItem(position: Int, isSmooth: Boolean) {
		pager.setCurrentItem(position, isSmooth)
	}

	override fun getCurrentPageScroll() = 0f

	override fun restorePageScroll(position: Int, scroll: Float) = Unit

	companion object {

		fun newInstance(state: ReaderState) = PagerReaderFragment().withArgs(1) {
			putParcelable(ARG_STATE, state)
		}
	}
}