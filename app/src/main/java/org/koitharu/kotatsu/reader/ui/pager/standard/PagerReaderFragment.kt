package org.koitharu.kotatsu.reader.ui.pager.standard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.absoluteValue
import kotlinx.coroutines.async
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.recyclerView
import org.koitharu.kotatsu.utils.ext.resetTransformations
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

@AndroidEntryPoint
class PagerReaderFragment : BaseReader<FragmentReaderStandardBinding>() {

	private var pagesAdapter: PagesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		pagesAdapter = PagesAdapter(viewModel.pageLoader, viewModel.readerSettings, exceptionResolver)
		with(binding.pager) {
			adapter = pagesAdapter
			offscreenPageLimit = 2
			doOnPageChanged(::notifyPageChanged)
		}

		viewModel.readerAnimation.observe(viewLifecycleOwner) {
			val transformer = if (it) PageAnimTransformer() else null
			binding.pager.setPageTransformer(transformer)
			if (transformer == null) {
				binding.pager.recyclerView?.children?.forEach { view ->
					view.resetTransformations()
				}
			}
		}
	}

	override fun onDestroyView() {
		pagesAdapter = null
		super.onDestroyView()
	}

	override fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		viewLifecycleScope.launchWhenCreated {
			val items = async {
				pagesAdapter?.setItems(pages)
			}
			if (pendingState != null) {
				val position = pages.indexOfFirst {
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

	override fun switchPageBy(delta: Int) {
		with(binding.pager) {
			setCurrentItem(currentItem + delta, true)
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		binding.pager.setCurrentItem(
			position,
			smooth && (binding.pager.currentItem - position).absoluteValue < SMOOTH_SCROLL_LIMIT,
		)
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
		viewModel.onCurrentPageChanged(page)
	}

	companion object {

		const val SMOOTH_SCROLL_LIMIT = 3
	}
}
