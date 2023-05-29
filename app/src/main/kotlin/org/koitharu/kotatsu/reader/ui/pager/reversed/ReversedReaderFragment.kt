package org.koitharu.kotatsu.reader.ui.pager.reversed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.doOnPageChanged
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.recyclerView
import org.koitharu.kotatsu.core.util.ext.resetTransformations
import org.koitharu.kotatsu.core.util.ext.viewLifecycleScope
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.standard.PagerReaderFragment
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class ReversedReaderFragment : BaseReaderFragment<FragmentReaderStandardBinding>() {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	private var pagerAdapter: ReversedPagesAdapter? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentReaderStandardBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		pagerAdapter = ReversedPagesAdapter(
			lifecycleOwner = viewLifecycleOwner,
			loader = pageLoader,
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
		requireViewBinding().pager.adapter = null
		super.onDestroyView()
	}

	override fun switchPageBy(delta: Int) {
		with(requireViewBinding().pager) {
			setCurrentItem(currentItem - delta, context.isAnimationsEnabled)
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		with(requireViewBinding().pager) {
			setCurrentItem(
				reversed(position),
				smooth && context.isAnimationsEnabled && (currentItem - position).absoluteValue < PagerReaderFragment.SMOOTH_SCROLL_LIMIT,
			)
		}
	}

	override fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		val reversedPages = pages.asReversed()
		viewLifecycleScope.launch {
			val items = async {
				pagerAdapter?.setItems(reversedPages)
			}
			if (pendingState != null) {
				val position = reversedPages.indexOfLast {
					it.chapterId == pendingState.chapterId && it.index == pendingState.page
				}
				items.await() ?: return@launch
				if (position != -1) {
					requireViewBinding().pager.setCurrentItem(position, false)
					notifyPageChanged(position)
				}
			} else {
				items.await()
			}
		}
	}

	override fun getCurrentState(): ReaderState? = viewBinding?.run {
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
