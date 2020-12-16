package org.koitharu.kotatsu.reader.ui.pager.reversed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.async
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.callOnPageChaneListeners
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.swapAdapter
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

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

		viewModel.readerAnimation.observe(viewLifecycleOwner) {
			val transformer = if (it) ReversedPageAnimTransformer() else null
			binding.pager.setPageTransformer(transformer)
		}
		viewModel.onZoomChanged.observe(viewLifecycleOwner) {
			pagerAdapter = ReversedPagesAdapter(loader, get())
			binding.pager.swapAdapter(pagerAdapter)
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
		val reversedPages = pages.asReversed()
		viewLifecycleScope.launchWhenCreated {
			val items = async {
				pagerAdapter?.setItems(reversedPages)
			}
			if (pendingState != null) {
				val position = reversedPages.indexOfLast {
					it.chapterId == pendingState.chapterId && it.index == pendingState.page
				}
				items.await() ?: return@launchWhenCreated
				if (position != -1) {
					binding.pager.setCurrentItem(position, false)
				}
			} else {
				items.await()
			}
			binding.pager.post {
				bindingOrNull()?.pager?.callOnPageChaneListeners()
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