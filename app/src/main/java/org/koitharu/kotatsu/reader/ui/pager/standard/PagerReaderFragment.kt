package org.koitharu.kotatsu.reader.ui.pager.standard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import kotlinx.coroutines.async
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReader
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.*

class PagerReaderFragment : BaseReader<FragmentReaderStandardBinding>() {

	private var pagesAdapter: PagesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		pagesAdapter = PagesAdapter(loader, get())
		with(binding.pager) {
			adapter = pagesAdapter
			offscreenPageLimit = 1
			doOnPageChanged(::notifyPageChanged)
		}

		viewModel.readerAnimation.observe(viewLifecycleOwner) {
			val transformer = if (it) PageAnimTransformer() else null
			binding.pager.setPageTransformer(transformer)
			if (transformer == null) {
				binding.pager.recyclerView?.children?.forEach {
					it.resetTransformations()
				}
			}
		}
		viewModel.onZoomChanged.observe(viewLifecycleOwner) {
			pagesAdapter?.notifyDataSetChanged()
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
				}
			} else {
				items.await()
			}
			binding.pager.post {
				bindingOrNull()?.pager?.callOnPageChaneListeners()
			}
		}
	}

	override fun switchPageBy(delta: Int) {
		with(binding.pager) {
			setCurrentItem(currentItem + delta, true)
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		binding.pager.setCurrentItem(position, smooth)
	}

	override fun getCurrentState(): ReaderState? = bindingOrNull()?.run {
		val adapter = pager.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(pager.currentItem) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0
		)
	}

	private fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(page)
	}
}