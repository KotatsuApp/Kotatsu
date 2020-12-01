package org.koitharu.kotatsu.reader.ui.wetoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import org.koitharu.kotatsu.databinding.FragmentReaderWebtoonBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.base.AbstractReader
import org.koitharu.kotatsu.reader.ui.base.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.base.ReaderPage
import org.koitharu.kotatsu.utils.ext.doOnCurrentItemChanged
import org.koitharu.kotatsu.utils.ext.findCenterViewPosition
import org.koitharu.kotatsu.utils.ext.firstItem
import org.koitharu.kotatsu.utils.ext.withArgs

class WebtoonReaderFragment : AbstractReader<FragmentReaderWebtoonBinding>() {

	private val scrollInterpolator = AccelerateDecelerateInterpolator()
	private var paginationListener: ListPaginationListener? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentReaderWebtoonBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		paginationListener = ListPaginationListener(2, this)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = readerAdapter
			addOnScrollListener(paginationListener!!)
			doOnCurrentItemChanged(::notifyPageChanged)
		}
	}

	override fun onCreateAdapter(dataSet: List<ReaderPage>): BaseReaderAdapter {
		return WebtoonAdapter(dataSet, loader)
	}

	override fun recreateAdapter() {
		super.recreateAdapter()
		binding.recyclerView.swapAdapter(readerAdapter, true)
	}

	override fun onDestroyView() {
		paginationListener = null
		super.onDestroyView()
	}

	override fun getCurrentItem(): Int {
		return binding.recyclerView.findCenterViewPosition()
	}

	override fun setCurrentItem(position: Int, isSmooth: Boolean) {
		if (isSmooth) {
			binding.recyclerView.smoothScrollToPosition(position)
		} else {
			binding.recyclerView.firstItem = position
		}
	}

	override fun switchPageBy(delta: Int) {
		binding.recyclerView.smoothScrollBy(
			0,
			(binding.recyclerView.height * 0.9).toInt() * delta,
			scrollInterpolator
		)
	}

	override fun getCurrentPageScroll(): Int {
		return (binding.recyclerView.findViewHolderForAdapterPosition(getCurrentItem()) as? WebtoonHolder)
			?.getScrollY() ?: 0
	}

	override fun restorePageScroll(position: Int, scroll: Int) {
		binding.recyclerView.post {
			val holder = binding.recyclerView.findViewHolderForAdapterPosition(position) ?: return@post
			(holder as WebtoonHolder).restoreScroll(scroll)
		}
	}

	companion object {

		fun newInstance(state: ReaderState) = WebtoonReaderFragment().withArgs(1) {
			putParcelable(ARG_STATE, state)
		}
	}
}