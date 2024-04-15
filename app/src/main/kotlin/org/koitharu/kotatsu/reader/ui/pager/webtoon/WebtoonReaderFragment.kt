package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.ui.list.lifecycle.RecyclerViewLifecycleDispatcher
import org.koitharu.kotatsu.core.util.ext.findCenterViewPosition
import org.koitharu.kotatsu.core.util.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.removeItemDecoration
import org.koitharu.kotatsu.databinding.FragmentReaderWebtoonBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import javax.inject.Inject

@AndroidEntryPoint
class WebtoonReaderFragment : BaseReaderFragment<FragmentReaderWebtoonBinding>(),
	WebtoonRecyclerView.OnWebtoonScrollListener {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	private val scrollInterpolator = DecelerateInterpolator()

	private var recyclerLifecycleDispatcher: RecyclerViewLifecycleDispatcher? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderWebtoonBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentReaderWebtoonBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = readerAdapter
			addOnPageScrollListener(this@WebtoonReaderFragment)
			recyclerLifecycleDispatcher = RecyclerViewLifecycleDispatcher().also {
				addOnScrollListener(it)
			}
		}
		viewModel.isWebtoonZooEnabled.observe(viewLifecycleOwner) {
			binding.frame.isZoomEnable = it
		}
		viewModel.defaultWebtoonZoomOut.take(1).observe(viewLifecycleOwner) {
			binding.frame.zoom = 1f - it
		}
		viewModel.isWebtoonGapsEnabled.observe(viewLifecycleOwner) {
			val rv = binding.recyclerView
			rv.removeItemDecoration(WebtoonGapsDecoration::class.java)
			if (it) {
				rv.addItemDecoration(WebtoonGapsDecoration())
			}
		}
		viewModel.readerSettings.observe(viewLifecycleOwner) {
			it.applyBackground(binding.root)
		}
	}

	override fun onDestroyView() {
		recyclerLifecycleDispatcher = null
		requireViewBinding().recyclerView.adapter = null
		super.onDestroyView()
	}

	override fun onCreateAdapter() = WebtoonAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun onScrollChanged(
		recyclerView: WebtoonRecyclerView,
		dy: Int,
		firstVisiblePosition: Int,
		lastVisiblePosition: Int,
	) {
		viewModel.onCurrentPageChanged(firstVisiblePosition, lastVisiblePosition)
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) = coroutineScope {
		val setItems = launch {
			requireAdapter().setItems(pages)
			yield()
			viewBinding?.recyclerView?.let { rv ->
				recyclerLifecycleDispatcher?.invalidate(rv)
			}
		}
		if (pendingState != null) {
			val position = pages.indexOfFirst {
				it.chapterId == pendingState.chapterId && it.index == pendingState.page
			}
			setItems.join()
			if (position != -1) {
				with(requireViewBinding().recyclerView) {
					firstVisibleItemPosition = position
					post {
						(findViewHolderForAdapterPosition(position) as? WebtoonHolder)
							?.restoreScroll(pendingState.scroll)
					}
				}
				viewModel.onCurrentPageChanged(position, position)
			} else {
				Snackbar.make(requireView(), R.string.not_found_404, Snackbar.LENGTH_SHORT)
					.show()
			}
		} else {
			setItems.join()
		}
	}

	override fun getCurrentState(): ReaderState? = viewBinding?.run {
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

	override fun onZoomIn() {
		viewBinding?.frame?.onZoomIn()
	}

	override fun onZoomOut() {
		viewBinding?.frame?.onZoomOut()
	}

	override fun switchPageBy(delta: Int) {
		with(requireViewBinding().recyclerView) {
			if (isAnimationEnabled()) {
				smoothScrollBy(0, (height * 0.9).toInt() * delta, scrollInterpolator)
			} else {
				nestedScrollBy(0, (height * 0.9).toInt() * delta)
			}
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		requireViewBinding().recyclerView.firstVisibleItemPosition = position
	}

	override fun scrollBy(delta: Int, smooth: Boolean): Boolean {
		if (smooth && isAnimationEnabled()) {
			requireViewBinding().recyclerView.smoothScrollBy(0, delta, scrollInterpolator)
		} else {
			requireViewBinding().recyclerView.nestedScrollBy(0, delta)
		}
		return true
	}
}
