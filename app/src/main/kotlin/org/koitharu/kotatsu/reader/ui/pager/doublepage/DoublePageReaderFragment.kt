package org.koitharu.kotatsu.reader.ui.pager.doublepage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.firstVisibleItemPosition
import org.koitharu.kotatsu.databinding.FragmentReaderDoubleBinding
import org.koitharu.kotatsu.parsers.util.toIntUp
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class DoublePageReaderFragment : BaseReaderFragment<FragmentReaderDoubleBinding>() {

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
			addOnScrollListener(PageScrollListener())
			DoublePageSnapHelper().attachToRecyclerView(this)
		}
	}

	override fun onDestroyView() {
		requireViewBinding().recyclerView.adapter = null
		super.onDestroyView()
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) =
		coroutineScope {
			val items = async {
				requireAdapter().setItems(pages)
				yield()
			}
			if (pendingState != null) {
				val position = pages.indexOfFirst {
					it.chapterId == pendingState.chapterId && it.index == pendingState.page
				}
				items.await()
				if (position != -1) {
					requireViewBinding().recyclerView.firstVisibleItemPosition = position or 1
					notifyPageChanged(position)
				} else {
					Snackbar.make(requireView(), R.string.not_found_404, Snackbar.LENGTH_SHORT)
						.show()
				}
			} else {
				items.await()
			}
		}

	override fun onCreateAdapter() = DoublePagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

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

	private fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(page)
	}

	private fun RecyclerView.currentItem(): Int {
		val lm = layoutManager as LinearLayoutManager
		return ((lm.findFirstVisibleItemPosition() + lm.findLastVisibleItemPosition()) / 2f).toIntUp()
	}

	private inner class PageScrollListener : RecyclerView.OnScrollListener() {

		private var lastPage = RecyclerView.NO_POSITION

		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			super.onScrolled(recyclerView, dx, dy)
			val page = recyclerView.currentItem()
			if (page != lastPage) {
				lastPage = page
				notifyPageChanged(page)
			}
		}
	}
}
