package org.koitharu.kotatsu.reader.ui.pager.doublepage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.databinding.FragmentReaderDoubleBinding
import org.koitharu.kotatsu.parsers.util.toIntUp
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.ReaderViewModel
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.standard.PageHolder
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class DoubleReaderFragment : BaseReaderFragment<FragmentReaderDoubleBinding>() {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderDoubleBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(
		binding: FragmentReaderDoubleBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		with(binding.recyclerView) {
			adapter = readerAdapter
			addOnScrollListener(PageScrollListener(viewModel))
			DoublePageSnapHelper().attachToRecyclerView(this)
		}
	}

	override fun onDestroyView() {
		requireViewBinding().recyclerView.adapter = null
		super.onDestroyView()
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) = coroutineScope {
		val items = launch {
			requireAdapter().setItems(pages)
			yield()
		}
		if (pendingState != null) {
			var position = pages.indexOfFirst {
				it.chapterId == pendingState.chapterId && it.index == pendingState.page
			}
			items.join()
			if (position != -1) {
				position = position or 1
				requireViewBinding().recyclerView.firstVisibleItemPosition = position
				viewModel.onCurrentPageChanged(position, position + 1)
			} else {
				Snackbar.make(requireView(), R.string.not_found_404, Snackbar.LENGTH_SHORT)
					.show()
			}
		} else {
			items.join()
		}
	}

	override fun onCreateAdapter() = DoublePagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun onZoomIn() {
		(viewBinding ?: return).recyclerView.pageHolders()
			.forEach { it.onZoomIn() }
	}

	override fun onZoomOut() {
		(viewBinding ?: return).recyclerView.pageHolders()
			.forEach { it.onZoomOut() }
	}

	override fun switchPageBy(delta: Int) {
		switchPageTo((requireViewBinding().recyclerView.currentItem() + delta) or 1, delta.absoluteValue > 1)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		requireViewBinding().recyclerView.firstVisibleItemPosition = position or 1
	}

	override fun getCurrentState(): ReaderState? = viewBinding?.run {
		val adapter = recyclerView.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(recyclerView.currentItem()) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0,
		)
	}

	private fun RecyclerView.currentItem(): Int {
		val lm = layoutManager as LinearLayoutManager
		return ((lm.findFirstVisibleItemPosition() + lm.findLastVisibleItemPosition()) / 2f).toIntUp()
	}

	private fun RecyclerView.pageHolders(): Sequence<PageHolder> {
		val lm = layoutManager as? LinearLayoutManager ?: return emptySequence()
		return (lm.findFirstVisibleItemPosition()..lm.findLastVisibleItemPosition()).asSequence()
			.mapNotNull { findViewHolderForAdapterPosition(it) as? PageHolder }
	}

	private class PageScrollListener(
		private val viewModel: ReaderViewModel,
	) : RecyclerView.OnScrollListener() {

		private var firstPos = RecyclerView.NO_POSITION
		private var lastPos = RecyclerView.NO_POSITION

		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			super.onScrolled(recyclerView, dx, dy)
			val lm = recyclerView.layoutManager as? LinearLayoutManager
			if (lm == null) {
				firstPos = RecyclerView.NO_POSITION
				lastPos = RecyclerView.NO_POSITION
				return
			}
			val newFirstPos = lm.findFirstVisibleItemPosition()
			val newLastPos = lm.findLastVisibleItemPosition()
			if (newFirstPos != firstPos || newLastPos != lastPos) {
				firstPos = newFirstPos
				lastPos = newLastPos
				viewModel.onCurrentPageChanged(newFirstPos, newLastPos)
			}
		}
	}
}
