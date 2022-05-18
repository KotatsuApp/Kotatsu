package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.displayCompat
import com.google.android.material.R as materialR

abstract class BaseBottomSheet<B : ViewBinding> : BottomSheetDialogFragment() {

	private var viewBinding: B? = null

	protected val binding: B
		get() = checkNotNull(viewBinding)

	protected val behavior: BottomSheetBehavior<*>?
		get() = (dialog as? BottomSheetDialog)?.behavior

	final override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val binding = onInflateView(inflater, container)
		viewBinding = binding

		// Enforce max width for tablets
		val width = resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
		if (width > 0) {
			behavior?.maxWidth = width
		}

		// Set peek height to 50% display height
		requireContext().displayCompat?.let {
			val metrics = DisplayMetrics()
			it.getRealMetrics(metrics)
			behavior?.peekHeight = metrics.heightPixels / 2
		}

		return binding.root
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B

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
}