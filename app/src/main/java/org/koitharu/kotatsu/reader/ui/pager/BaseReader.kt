package org.koitharu.kotatsu.reader.ui.pager

import android.os.Bundle
import android.view.View
import androidx.core.graphics.Insets
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.ReaderViewModel

abstract class BaseReader<B : ViewBinding> : BaseFragment<B>() {

	protected val viewModel by sharedViewModel<ReaderViewModel>()
	protected val loader by lazy(LazyThreadSafetyMode.NONE) {
		PageLoader(lifecycleScope, get(), get())
	}
	private var lastReaderState: ReaderState? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		lastReaderState = savedInstanceState?.getParcelable(KEY_STATE) ?: lastReaderState

		viewModel.content.observe(viewLifecycleOwner) {
			onPagesChanged(it.pages, lastReaderState ?: it.state)
			lastReaderState = null
		}
	}

	override fun onDestroyView() {
		lastReaderState = getCurrentState()
		super.onDestroyView()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		getCurrentState()?.let {
			lastReaderState = it
		}
		outState.putParcelable(KEY_STATE, lastReaderState)
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	abstract fun switchPageBy(delta: Int)

	abstract fun switchPageTo(position: Int, smooth: Boolean)

	abstract fun getCurrentState(): ReaderState?

	protected abstract fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?)

	private companion object {

		const val KEY_STATE = "state"
	}
}