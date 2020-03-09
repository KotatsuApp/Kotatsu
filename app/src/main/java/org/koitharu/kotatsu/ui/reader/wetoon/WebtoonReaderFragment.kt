package org.koitharu.kotatsu.ui.reader.wetoon

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_reader_webtoon.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.reader.ReaderState
import org.koitharu.kotatsu.ui.reader.base.AbstractReader
import org.koitharu.kotatsu.ui.reader.base.BaseReaderAdapter
import org.koitharu.kotatsu.ui.reader.base.GroupedList
import org.koitharu.kotatsu.ui.reader.standard.PagerReaderFragment
import org.koitharu.kotatsu.utils.ext.doOnCurrentItemChanged
import org.koitharu.kotatsu.utils.ext.findMiddleVisibleItemPosition
import org.koitharu.kotatsu.utils.ext.firstItem
import org.koitharu.kotatsu.utils.ext.withArgs

class WebtoonReaderFragment : AbstractReader(R.layout.fragment_reader_webtoon) {

	private val scrollInterpolator = AccelerateDecelerateInterpolator()
	protected var paginationListener: ListPaginationListener? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		paginationListener = ListPaginationListener(2, this)
		recyclerView.setHasFixedSize(true)
		recyclerView.adapter = adapter
		recyclerView.addOnScrollListener(paginationListener!!)
		recyclerView.doOnCurrentItemChanged(::notifyPageChanged)
	}

	override fun onCreateAdapter(dataSet: GroupedList<Long, MangaPage>): BaseReaderAdapter<*> {
		return WebtoonAdapter(dataSet, loader)
	}

	override fun onDestroyView() {
		paginationListener = null
		super.onDestroyView()
	}

	override val itemsCount: Int
		get() = adapter?.itemCount ?: 0

	override fun getCurrentItem(): Int {
		return (recyclerView.layoutManager as LinearLayoutManager).findMiddleVisibleItemPosition()
	}

	override fun setCurrentItem(position: Int, isSmooth: Boolean) {
		if (isSmooth) {
			recyclerView.smoothScrollToPosition(position)
		} else {
			recyclerView.firstItem = position
		}
	}

	override fun switchPageBy(delta: Int) {
		recyclerView.smoothScrollBy(0, (recyclerView.height * 0.9).toInt() * delta, scrollInterpolator)
	}

	companion object {

		fun newInstance(state: ReaderState) = WebtoonReaderFragment().withArgs(1) {
			putParcelable(ARG_STATE, state)
		}
	}
}