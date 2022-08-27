package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import org.koitharu.kotatsu.databinding.FragmentReaderWebtoonBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.findCenterViewPosition
import org.koitharu.kotatsu.utils.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

@AndroidEntryPoint
class WebtoonReaderFragment : BaseReader<FragmentReaderWebtoonBinding>() {

	private val scrollInterpolator = AccelerateDecelerateInterpolator()
	private var webtoonAdapter: WebtoonAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderWebtoonBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		webtoonAdapter = WebtoonAdapter(viewModel.pageLoader, viewModel.readerSettings, exceptionResolver)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = webtoonAdapter
			addOnPageScrollListener(PageScrollListener())
		}
	}

	override fun onDestroyView() {
		webtoonAdapter = null
		super.onDestroyView()
	}

	override fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		viewLifecycleScope.launchWhenCreated {
			val setItems = async { webtoonAdapter?.setItems(pages) }
			if (pendingState != null) {
				val position = pages.indexOfFirst {
					it.chapterId == pendingState.chapterId && it.index == pendingState.page
				}
				setItems.await() ?: return@launchWhenCreated
				if (position != -1) {
					with(binding.recyclerView) {
						firstVisibleItemPosition = position
						post {
							(findViewHolderForAdapterPosition(position) as? WebtoonHolder)
								?.restoreScroll(pendingState.scroll)
						}
					}
					notifyPageChanged(position)
				}
			} else {
				setItems.await()
			}
		}
	}

	override fun getCurrentState(): ReaderState? = bindingOrNull()?.run {
		val currentItem = recyclerView.findCenterViewPosition()
		val adapter = recyclerView.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(currentItem) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = (recyclerView.findViewHolderForAdapterPosition(currentItem) as? WebtoonHolder)
				?.getScrollY() ?: 0,
		)
	}

	private fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(page)
	}

	override fun switchPageBy(delta: Int) {
		binding.recyclerView.smoothScrollBy(
			0,
			(binding.recyclerView.height * 0.9).toInt() * delta,
			scrollInterpolator,
		)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		binding.recyclerView.firstVisibleItemPosition = position
	}

	private inner class PageScrollListener : WebtoonRecyclerView.OnPageScrollListener() {

		override fun onPageChanged(recyclerView: WebtoonRecyclerView, index: Int) {
			super.onPageChanged(recyclerView, index)
			notifyPageChanged(index)
		}
	}
}
