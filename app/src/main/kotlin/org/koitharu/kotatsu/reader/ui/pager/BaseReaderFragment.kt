package org.koitharu.kotatsu.reader.ui.pager

import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.prefs.ReaderAnimation
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.widgets.ZoomControl
import org.koitharu.kotatsu.core.util.ext.getParcelableCompat
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.ReaderViewModel

abstract class BaseReaderFragment<B : ViewBinding> : BaseFragment<B>(), ZoomControl.ZoomControlListener {

	protected val viewModel by activityViewModels<ReaderViewModel>()

	protected var readerAdapter: BaseReaderAdapter<*>? = null
		private set

	override fun onViewBindingCreated(binding: B, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		readerAdapter = onCreateAdapter()

		viewModel.content.observe(viewLifecycleOwner) {
			if (it.state == null && it.pages.isNotEmpty() && readerAdapter?.hasItems != true) {
				onPagesChanged(it.pages, viewModel.getCurrentState())
			} else {
				onPagesChanged(it.pages, it.state)
			}
		}
	}

	override fun onPause() {
		super.onPause()
		viewModel.saveCurrentState(getCurrentState())
	}

	override fun onDestroyView() {
		viewModel.saveCurrentState(getCurrentState())
		readerAdapter = null
		super.onDestroyView()
	}

	protected fun requireAdapter() = checkNotNull(readerAdapter) {
		"Adapter was not created or already destroyed"
	}

	protected fun isAnimationEnabled(): Boolean {
		return context?.isAnimationsEnabled == true && viewModel.pageAnimation.value != ReaderAnimation.NONE
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	abstract fun switchPageBy(delta: Int)

	abstract fun switchPageTo(position: Int, smooth: Boolean)

	open fun scrollBy(delta: Int, smooth: Boolean): Boolean = false

	abstract fun getCurrentState(): ReaderState?

	protected abstract fun onCreateAdapter(): BaseReaderAdapter<*>

	protected abstract suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?)
}
