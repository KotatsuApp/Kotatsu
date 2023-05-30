package org.koitharu.kotatsu.core.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.activity.OnBackPressedDispatcher
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.AppBottomSheetDialog
import org.koitharu.kotatsu.core.util.ext.findActivity
import org.koitharu.kotatsu.core.util.ext.getDisplaySize
import com.google.android.material.R as materialR

@Deprecated(
	"Use BaseAdaptiveSheet",
	replaceWith = ReplaceWith("BaseAdaptiveSheet<B>", "org.koitharu.kotatsu.core.ui.sheet.BaseAdaptiveSheet"),
)
abstract class BaseBottomSheet<B : ViewBinding> : BottomSheetDialogFragment() {

	var viewBinding: B? = null
		private set

	@Deprecated("", ReplaceWith("requireViewBinding()"))
	protected val binding: B
		get() = requireViewBinding()

	protected val behavior: BottomSheetBehavior<*>?
		get() = (dialog as? BottomSheetDialog)?.behavior

	val isExpanded: Boolean
		get() = behavior?.state == BottomSheetBehavior.STATE_EXPANDED

	val onBackPressedDispatcher: OnBackPressedDispatcher
		get() = (requireDialog() as AppBottomSheetDialog).onBackPressedDispatcher

	final override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		val binding = onCreateViewBinding(inflater, container)
		viewBinding = binding
		return binding.root
	}

	final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val binding = requireViewBinding()
		// Enforce max width for tablets
		val width = resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
		if (width > 0) {
			behavior?.maxWidth = width
		}
		// Set peek height to 40% display height
		binding.root.context.findActivity()?.getDisplaySize()?.let {
			behavior?.peekHeight = (it.height() * 0.4).toInt()
		}
		onViewBindingCreated(binding, savedInstanceState)
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return AppBottomSheetDialog(requireContext(), theme)
	}

	fun addBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback) {
		val b = behavior ?: return
		b.addBottomSheetCallback(callback)
		val rootView = dialog?.findViewById<View>(materialR.id.design_bottom_sheet)
		if (rootView != null) {
			callback.onStateChanged(rootView, b.state)
		}
	}

	protected abstract fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

	protected open fun onViewBindingCreated(binding: B, savedInstanceState: Bundle?) = Unit

	protected fun setExpanded(isExpanded: Boolean, isLocked: Boolean) {
		val b = behavior ?: return
		if (isExpanded) {
			b.state = BottomSheetBehavior.STATE_EXPANDED
		}
		b.isFitToContents = !isExpanded
		val rootView = dialog?.findViewById<View>(materialR.id.design_bottom_sheet)
		rootView?.updateLayoutParams {
			height = if (isExpanded) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
		}
		b.isDraggable = !isLocked
	}

	fun requireViewBinding(): B = checkNotNull(viewBinding) {
		"Fragment $this did not return a ViewBinding from onCreateView() or this was called before onCreateView()."
	}
}
