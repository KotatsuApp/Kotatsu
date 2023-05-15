package org.koitharu.kotatsu.reader.ui.pager.standard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.doOnPageChanged
import org.koitharu.kotatsu.utils.ext.isAnimationsEnabled
import org.koitharu.kotatsu.utils.ext.recyclerView
import org.koitharu.kotatsu.utils.ext.resetTransformations
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class PagerReaderFragment : BaseReaderFragment<FragmentReaderStandardBinding>() {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	private var pagesAdapter: PagesAdapter? = null

	override fun onInflateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	@SuppressLint("NotifyDataSetChanged")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		pagesAdapter = PagesAdapter(
			lifecycleOwner = viewLifecycleOwner,
			loader = pageLoader,
			settings = viewModel.readerSettings,
			networkState = networkState,
			exceptionResolver = exceptionResolver,
		)
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
		binding.pager.adapter = null
		super.onDestroyView()
	}

	override fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		viewLifecycleScope.launch {
			val items = async {
				pagesAdapter?.setItems(pages)
			}
			if (pendingState != null) {
				val position = pages.indexOfFirst {
					it.chapterId == pendingState.chapterId && it.index == pendingState.page
				}
				items.await() ?: return@launch
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
			setCurrentItem(currentItem + delta, context.isAnimationsEnabled)
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		with(binding.pager) {
			setCurrentItem(
				position,
				smooth && context.isAnimationsEnabled && (currentItem - position).absoluteValue < SMOOTH_SCROLL_LIMIT,
			)
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
		viewModel.onCurrentPageChanged(page)
	}

	companion object {

		const val SMOOTH_SCROLL_LIMIT = 3
	}
}
