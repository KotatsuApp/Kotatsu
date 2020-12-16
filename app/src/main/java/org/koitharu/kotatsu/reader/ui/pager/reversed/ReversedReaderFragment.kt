package org.koitharu.kotatsu.reader.ui.pager.reversed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.doOnPageChanged

class ReversedReaderFragment : BaseReader<FragmentReaderStandardBinding>() {

	private var pagerAdapter: ReversedPagesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		pagerAdapter = ReversedPagesAdapter(loader, get())
		with(binding.pager) {
			adapter = pagerAdapter
			offscreenPageLimit = 2
			doOnPageChanged(::notifyPageChanged)
		}
	}

	override fun onDestroyView() {
		pagerAdapter = null
		super.onDestroyView()
	}

	override fun switchPageBy(delta: Int) {
		with(binding.pager) {
			setCurrentItem(currentItem - delta, true)
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		binding.pager.setCurrentItem(reversed(position), smooth)
	}

	override fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		pagerAdapter?.setItems(pages.asReversed()) {
			if (pendingState != null) {
				val position = pages.indexOfFirst {
					it.chapterId == pendingState.chapterId && it.index == pendingState.page
				}
				if (position == -1) return@setItems
				binding.pager.setCurrentItem(position, false)
			}
		}
	}

	override fun getCurrentState(): ReaderState? = bindingOrNull()?.run {
		val adapter = pager.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(pager.currentItem) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0
		)
	}

	private fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(reversed(page))
	}

	private fun reversed(position: Int): Int {
		return ((pagerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}
}