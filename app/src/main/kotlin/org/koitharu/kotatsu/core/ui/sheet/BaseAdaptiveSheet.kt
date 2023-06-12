package org.koitharu.kotatsu.core.ui.sheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.sidesheet.SideSheetDialog
import org.koitharu.kotatsu.R
import com.google.android.material.R as materialR

abstract class BaseAdaptiveSheet<B : ViewBinding> : AppCompatDialogFragment() {

	private var waitingForDismissAllowingStateLoss = false
	private var isFitToContentsDisabled = false

	var viewBinding: B? = null
		private set

	@Deprecated("", ReplaceWith("requireViewBinding()"))
	protected val binding: B
		get() = requireViewBinding()

	protected val behavior: AdaptiveSheetBehavior?
		get() = AdaptiveSheetBehavior.from(dialog)

	val isExpanded: Boolean
		get() = behavior?.state == AdaptiveSheetBehavior.STATE_EXPANDED

	val onBackPressedDispatcher: OnBackPressedDispatcher
		get() = requireComponentDialog().onBackPressedDispatcher

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
		onViewBindingCreated(binding, savedInstanceState)
	}

	override fun onDestroyView() {
		viewBinding = null
		super.onDestroyView()
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val context = requireContext()
		return if (context.resources.getBoolean(R.bool.is_tablet)) {
			SideSheetDialog(context, theme)
		} else {
			BottomSheetDialog(context, theme)
		}
	}

	fun addSheetCallback(callback: AdaptiveSheetCallback) {
		val b = behavior ?: return
		b.addCallback(callback)
		val rootView = dialog?.findViewById<View>(materialR.id.design_bottom_sheet)
			?: dialog?.findViewById(materialR.id.coordinator)
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
		if (b is AdaptiveSheetBehavior.Bottom) {
			b.isFitToContents = !isFitToContentsDisabled && !isExpanded
			val rootView = dialog?.findViewById<View>(materialR.id.design_bottom_sheet)
			rootView?.updateLayoutParams {
				height = if (isFitToContentsDisabled || isExpanded) {
					LayoutParams.MATCH_PARENT
				} else {
					LayoutParams.WRAP_CONTENT
				}
			}
		}
		b.isDraggable = !isLocked
	}

	protected fun disableFitToContents() {
		isFitToContentsDisabled = true
		val b = behavior as? AdaptiveSheetBehavior.Bottom ?: return
		b.isFitToContents = false
		dialog?.findViewById<View>(materialR.id.design_bottom_sheet)?.updateLayoutParams {
			height = LayoutParams.MATCH_PARENT
		}
	}

	fun requireViewBinding(): B = checkNotNull(viewBinding) {
		"Fragment $this did not return a ViewBinding from onCreateView() or this was called before onCreateView()."
	}

	override fun dismiss() {
		if (!tryDismissWithAnimation(false)) {
			super.dismiss()
		}
	}

	override fun dismissAllowingStateLoss() {
		if (!tryDismissWithAnimation(true)) {
			super.dismissAllowingStateLoss()
		}
	}

	/**
	 * Tries to dismiss the dialog fragment with the bottom sheet animation. Returns true if possible,
	 * false otherwise.
	 */
	private fun tryDismissWithAnimation(allowingStateLoss: Boolean): Boolean {
		val shouldDismissWithAnimation = when (val dialog = dialog) {
			is BottomSheetDialog -> dialog.dismissWithAnimation
			is SideSheetDialog -> dialog.isDismissWithSheetAnimationEnabled
			else -> false
		}
		val behavior = behavior ?: return false
		return if (shouldDismissWithAnimation && behavior.isHideable) {
			dismissWithAnimation(behavior, allowingStateLoss)
			true
		} else {
			false
		}
	}

	private fun dismissWithAnimation(behavior: AdaptiveSheetBehavior, allowingStateLoss: Boolean) {
		waitingForDismissAllowingStateLoss = allowingStateLoss
		if (behavior.state == AdaptiveSheetBehavior.STATE_HIDDEN) {
			dismissAfterAnimation()
		} else {
			behavior.addCallback(SheetDismissCallback())
			behavior.state = AdaptiveSheetBehavior.STATE_HIDDEN
		}
	}

	private fun dismissAfterAnimation() {
		if (waitingForDismissAllowingStateLoss) {
			super.dismissAllowingStateLoss()
		} else {
			super.dismiss()
		}
	}

	private inner class SheetDismissCallback : AdaptiveSheetCallback {
		override fun onStateChanged(sheet: View, newState: Int) {
			if (newState == BottomSheetBehavior.STATE_HIDDEN) {
				dismissAfterAnimation()
			}
		}

		override fun onSlide(sheet: View, slideOffset: Float) {}
	}
}
