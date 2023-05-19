package org.koitharu.kotatsu.reader.ui.pager

import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.util.ext.getParcelableCompat
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.ReaderViewModel

private const val KEY_STATE = "state"

abstract class BaseReaderFragment<B : ViewBinding> : BaseFragment<B>() {

	protected val viewModel by activityViewModels<ReaderViewModel>()
	private var stateToSave: ReaderState? = null

	override fun onViewBindingCreated(binding: B, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		var restoredState = savedInstanceState?.getParcelableCompat<ReaderState>(KEY_STATE)

		viewModel.content.observe(viewLifecycleOwner) {
			onPagesChanged(it.pages, restoredState ?: it.state)
			restoredState = null
		}
	}

	override fun onPause() {
		super.onPause()
		viewModel.saveCurrentState(getCurrentState())
	}

	override fun onDestroyView() {
		stateToSave = getCurrentState()
		super.onDestroyView()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		getCurrentState()?.let {
			stateToSave = it
		}
		outState.putParcelable(KEY_STATE, stateToSave)
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	abstract fun switchPageBy(delta: Int)

	abstract fun switchPageTo(position: Int, smooth: Boolean)

	open fun scrollBy(delta: Int): Boolean = false

	abstract fun getCurrentState(): ReaderState?

	protected abstract fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?)
}
