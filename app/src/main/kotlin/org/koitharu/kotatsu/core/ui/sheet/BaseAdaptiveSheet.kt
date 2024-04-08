package org.koitharu.kotatsu.core.ui.sheet

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.ActionBarContextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.sidesheet.SideSheetDialog
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.util.ActionModeDelegate
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import com.google.android.material.R as materialR

abstract class BaseAdaptiveSheet<B : ViewBinding> : AppCompatDialogFragment() {

	private var waitingForDismissAllowingStateLoss = false
	private var isFitToContentsDisabled = false
	private var defaultStatusBarColor = Color.TRANSPARENT

	var viewBinding: B? = null
		private set

	@Deprecated("", ReplaceWith("requireViewBinding()"))
	protected val binding: B
		get() = requireViewBinding()

	protected val behavior: AdaptiveSheetBehavior?
		get() = AdaptiveSheetBehavior.from(dialog)

	@JvmField
	val actionModeDelegate = ActionModeDelegate()

	val isExpanded: Boolean
		get() = behavior?.state == AdaptiveSheetBehavior.STATE_EXPANDED

	val onBackPressedDispatcher: OnBackPressedDispatcher
		get() = (dialog as? ComponentDialog)?.onBackPressedDispatcher ?: requireActivity().onBackPressedDispatcher

	var isLocked = false
		private set
	private var lockCounter = 0

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
		val dialog = if (context.resources.getBoolean(R.bool.is_tablet)) {
			SideSheetDialogImpl(context, theme)
		} else {
			BottomSheetDialogImpl(context, theme)
		}
		dialog.onBackPressedDispatcher.addCallback(actionModeDelegate)
		return dialog
	}

	@CallSuper
	protected open fun dispatchSupportActionModeStarted(mode: ActionMode) {
		actionModeDelegate.onSupportActionModeStarted(mode)
		val ctx = requireContext()
		val actionModeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ColorUtils.compositeColors(
				ContextCompat.getColor(ctx, com.google.android.material.R.color.m3_appbar_overlay_color),
				ctx.getThemeColor(com.google.android.material.R.attr.colorSurface),
			)
		} else {
			ContextCompat.getColor(ctx, R.color.kotatsu_surface)
		}
		dialog?.window?.let {
			defaultStatusBarColor = it.statusBarColor
			it.statusBarColor = actionModeColor
		}
		val insets = ViewCompat.getRootWindowInsets(requireView())
			?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: return
		dialog?.window?.decorView?.findViewById<ActionBarContextView?>(androidx.appcompat.R.id.action_mode_bar)?.apply {
			setBackgroundColor(actionModeColor)
			updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = insets.top
			}
		}
	}

	@CallSuper
	protected open fun dispatchSupportActionModeFinished(mode: ActionMode) {
		actionModeDelegate.onSupportActionModeFinished(mode)
		dialog?.window?.statusBarColor = defaultStatusBarColor
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

	fun startSupportActionMode(callback: ActionMode.Callback): ActionMode? {
		val appCompatDialog = dialog as? AppCompatDialog ?: return null
		return appCompatDialog.delegate.startSupportActionMode(callback)
	}

	protected fun setExpanded(isExpanded: Boolean, isLocked: Boolean) {
		this.isLocked = isLocked
		if (!isLocked) {
			lockCounter = 0
		}
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

	@CallSuper
	open fun expandAndLock() {
		lockCounter++
		setExpanded(isExpanded = true, isLocked = true)
	}

	@CallSuper
	open fun unlock() {
		lockCounter--
		if (lockCounter <= 0) {
			setExpanded(isExpanded, false)
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

	private inner class SideSheetDialogImpl(context: Context, theme: Int) : SideSheetDialog(context, theme) {

		override fun onSupportActionModeStarted(mode: ActionMode?) {
			super.onSupportActionModeStarted(mode)
			if (mode != null) {
				dispatchSupportActionModeStarted(mode)
			}
		}

		override fun onSupportActionModeFinished(mode: ActionMode?) {
			super.onSupportActionModeFinished(mode)
			if (mode != null) {
				dispatchSupportActionModeFinished(mode)
			}
		}
	}

	private inner class BottomSheetDialogImpl(context: Context, theme: Int) : BottomSheetDialog(context, theme) {

		override fun onSupportActionModeStarted(mode: ActionMode?) {
			super.onSupportActionModeStarted(mode)
			if (mode != null) {
				dispatchSupportActionModeStarted(mode)
			}
		}

		override fun onSupportActionModeFinished(mode: ActionMode?) {
			super.onSupportActionModeFinished(mode)
			if (mode != null) {
				dispatchSupportActionModeFinished(mode)
			}
		}
	}
}
