package org.koitharu.kotatsu.reader.ui.pager.reversed

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.standard.PagerReaderFragment
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.recyclerView
import org.koitharu.kotatsu.utils.ext.resetTransformations
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class ReversedReaderFragment : BaseReader<FragmentReaderStandardBinding>() {

	@Inject
	lateinit var networkState: NetworkState

	private var pagerAdapter: ReversedPagesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		pagerAdapter = ReversedPagesAdapter(
			lifecycleOwner = viewLifecycleOwner,
			loader = viewModel.pageLoader,
			settings = viewModel.readerSettings,
			networkState = networkState,
			exceptionResolver = exceptionResolver,
		)
		with(binding.pager) {
			adapter = pagerAdapter
			offscreenPageLimit = 2
			doOnPageChanged(::notifyPageChanged)
		}

		viewModel.readerAnimation.observe(viewLifecycleOwner) {
			val transformer = if (it) ReversedPageAnimTransformer() else null
			binding.pager.setPageTransformer(transformer)
			if (transformer == null) {
				binding.pager.recyclerView?.children?.forEach { v ->
					v.resetTransformations()
				}
			}
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
		binding.pager.setCurrentItem(
			reversed(position),
			smooth && (binding.pager.currentItem - position).absoluteValue < PagerReaderFragment.SMOOTH_SCROLL_LIMIT,
		)
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
					notifyPageChanged(position)
				}
			} else {
				items.await()
			}
		}
	}

	override fun getCurrentState(): ReaderState? = bindingOrNull()?.run {
		val adapter = pager.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(pager.currentItem) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0,
		)
	}

	private fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(reversed(page))
	}

	private fun reversed(position: Int): Int {
		return ((pagerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}
}
