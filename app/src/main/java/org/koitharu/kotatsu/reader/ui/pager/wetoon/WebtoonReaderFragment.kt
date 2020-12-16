package org.koitharu.kotatsu.reader.ui.pager.wetoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import kotlinx.coroutines.async
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.databinding.FragmentReaderWebtoonBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.*

class WebtoonReaderFragment : BaseReader<FragmentReaderWebtoonBinding>() {

	private val scrollInterpolator = AccelerateDecelerateInterpolator()
	private var webtoonAdapter: WebtoonAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentReaderWebtoonBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		webtoonAdapter = WebtoonAdapter(loader, get())
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = webtoonAdapter
			doOnCurrentItemChanged(::notifyPageChanged)
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
						firstItem = position
						post {
							(findViewHolderForAdapterPosition(position) as? WebtoonHolder)
								?.restoreScroll(pendingState.scroll)
						}
					}
				}
			} else {
				setItems.await()
			}
			binding.recyclerView.post {
				binding.recyclerView.callOnScrollListeners()
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
				?.getScrollY() ?: 0
		)
	}

	private fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(page)
	}

	override fun switchPageBy(delta: Int) {
		binding.recyclerView.smoothScrollBy(
			0,
			(binding.recyclerView.height * 0.9).toInt() * delta,
			scrollInterpolator
		)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		if (smooth) {
			binding.recyclerView.smoothScrollToPosition(position)
		} else {
			binding.recyclerView.firstItem = position
		}
	}
}