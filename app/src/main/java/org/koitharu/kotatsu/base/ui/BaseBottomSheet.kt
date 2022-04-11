package org.koitharu.kotatsu.base.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import com.google.android.material.R as materialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koitharu.kotatsu.R

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
		return binding.root
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return if (resources.getBoolean(R.bool.is_tablet)) {
			AppCompatDialog(context, R.style.Theme_Kotatsu_Dialog)
		} else super.onCreateDialog(savedInstanceState)
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