package org.koitharu.kotatsu.reader.ui.pager.reversed

import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BasePagerReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import javax.inject.Inject

@AndroidEntryPoint
class ReversedReaderFragment : BasePagerReaderFragment() {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateAdvancedTransformer(): ViewPager2.PageTransformer = ReversedPageAnimTransformer()

	override fun onCreateAdapter() = ReversedPagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun onWheelScroll(axisValue: Float) {
		val value = if (settings.isReaderControlAlwaysLTR) -axisValue else axisValue
		super.onWheelScroll(value)
	}

	override fun switchPageBy(delta: Int) {
		super.switchPageBy(-delta)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		super.switchPageTo(reversed(position), smooth)
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		super.onPagesChanged(pages.reversed(), pendingState)
	}

	override fun notifyPageChanged(page: Int) {
		val pos = reversed(page)
		viewModel.onCurrentPageChanged(pos, pos)
	}

	private fun reversed(position: Int): Int {
		return ((readerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}
}
