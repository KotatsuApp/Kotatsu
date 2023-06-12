package org.koitharu.kotatsu.reader.ui.pager.standard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.doOnPageChanged
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.recyclerView
import org.koitharu.kotatsu.core.util.ext.resetTransformations
import org.koitharu.kotatsu.databinding.FragmentReaderStandardBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderAdapter
import org.koitharu.kotatsu.reader.ui.pager.BaseReaderFragment
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class PagerReaderFragment : BaseReaderFragment<FragmentReaderStandardBinding>() {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderStandardBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentReaderStandardBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		with(binding.pager) {
			adapter = readerAdapter
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
		requireViewBinding().pager.adapter = null
		super.onDestroyView()
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) = coroutineScope {
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
				requireViewBinding().pager.setCurrentItem(position, false)
				notifyPageChanged(position)
			} else {
				Snackbar.make(requireView(), R.string.not_found_404, Snackbar.LENGTH_SHORT)
					.show()
			}
		} else {
			items.await()
		}
	}

	override fun onCreateAdapter() = PagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun switchPageBy(delta: Int) {
		with(requireViewBinding().pager) {
			setCurrentItem(currentItem + delta, context.isAnimationsEnabled)
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		with(requireViewBinding().pager) {
			setCurrentItem(
				position,
				smooth && context.isAnimationsEnabled && (currentItem - position).absoluteValue < SMOOTH_SCROLL_LIMIT,
			)
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
		viewModel.onCurrentPageChanged(page)
	}

	companion object {

		const val SMOOTH_SCROLL_LIMIT = 3
	}
}
