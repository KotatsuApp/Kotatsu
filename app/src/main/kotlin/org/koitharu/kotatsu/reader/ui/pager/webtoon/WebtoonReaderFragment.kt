package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.ui.list.lifecycle.RecyclerViewLifecycleDispatcher
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
	WebtoonRecyclerView.OnWebtoonScrollListener,
	WebtoonRecyclerView.OnPullGestureListener {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	private val scrollInterpolator = DecelerateInterpolator()

	private var recyclerLifecycleDispatcher: RecyclerViewLifecycleDispatcher? = null
	private var canGoPrev = true
	private var canGoNext = true

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
			setOnPullGestureListener(this@WebtoonReaderFragment)
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
		viewModel.readerSettingsProducer.observe(viewLifecycleOwner) {
			it.applyBackground(binding.root)
		}
		viewModel.isWebtoonPullGestureEnabled.observe(viewLifecycleOwner) { enabled ->
			binding.recyclerView.isPullGestureEnabled = enabled
		}
		viewModel.uiState.observe(viewLifecycleOwner) { state ->
			if (state != null) {
				canGoPrev = state.chapterIndex > 0
				canGoNext = state.chapterIndex < state.chaptersTotal - 1
			} else {
				canGoPrev = true
				canGoNext = true
			}
		}
	}

	override fun onDestroyView() {
		recyclerLifecycleDispatcher = null
		requireViewBinding().recyclerView.adapter = null
		super.onDestroyView()
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val offsetInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding?.apply {
			feedbackTop.updateLayoutParams<MarginLayoutParams> {
				topMargin = bottomMargin + offsetInsets.top
			}
			feedbackBottom.updateLayoutParams<MarginLayoutParams> {
				bottomMargin = topMargin + offsetInsets.bottom
			}
		}
		return super.onApplyWindowInsets(v, insets)
	}

	override fun onCreateAdapter() = WebtoonAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		readerSettingsProducer = viewModel.readerSettingsProducer,
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
		val currentItem = recyclerView.findCurrentPagePosition()
		val adapter = recyclerView.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(currentItem) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = (recyclerView.findViewHolderForAdapterPosition(currentItem) as? WebtoonHolder)?.getScrollY() ?: 0,
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

	override fun onPullProgressTop(progress: Float) {
		val binding = viewBinding ?: return
		if (canGoPrev) {
			binding.feedbackTop.setFeedbackText(getString(R.string.pull_to_prev_chapter))
		} else {
			binding.feedbackTop.setFeedbackText(getString(R.string.pull_top_no_prev))
		}
		binding.feedbackTop.updateFeedback(progress)
	}

	override fun onPullProgressBottom(progress: Float) {
		val binding = viewBinding ?: return
		if (canGoNext) {
			binding.feedbackBottom.setFeedbackText(getString(R.string.pull_to_next_chapter))
		} else {
			binding.feedbackBottom.setFeedbackText(getString(R.string.pull_bottom_no_next))
		}
		binding.feedbackBottom.updateFeedback(progress)
	}

	override fun onPullTriggeredTop() {
		(viewBinding ?: return).feedbackTop.fadeOut()
		if (canGoPrev) {
			viewModel.switchChapterBy(-1)
		}
	}

	override fun onPullTriggeredBottom() {
		(viewBinding ?: return).feedbackBottom.fadeOut()
		if (canGoNext) {
			viewModel.switchChapterBy(1)
		}
	}

	override fun onPullCancelled() {
		viewBinding?.apply {
			feedbackTop.fadeOut()
			feedbackBottom.fadeOut()
		}
	}

	private fun RecyclerView.findCurrentPagePosition(): Int {
		val centerX = width / 2f
		val centerY = height - resources.getDimension(R.dimen.webtoon_pages_gap)
		if (centerY <= 0) {
			return RecyclerView.NO_POSITION
		}
		val view = findChildViewUnder(centerX, centerY) ?: return RecyclerView.NO_POSITION
		return getChildAdapterPosition(view)
	}

	private fun TextView.updateFeedback(progress: Float) {
		val clamped = progress.coerceIn(0f, 1.2f)
		this.alpha = clamped.coerceAtMost(1f)
		this.scaleX = 0.9f + 0.1f * clamped.coerceAtMost(1f)
		this.scaleY = this.scaleX
	}

	private fun TextView.fadeOut() {
		animate().alpha(0f).setDuration(150L).start()
	}

	private fun TextView.setFeedbackText(text: CharSequence) {
		if (this.alpha <= 0f && text.isNotEmpty()) {
			this.alpha = 0f
			this.text = text
			animate().alpha(1f).setDuration(120L).start()
		} else {
			this.text = text
		}
	}
}
