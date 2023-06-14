package org.koitharu.kotatsu.core.ui.sheet

import android.app.Dialog
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.sidesheet.SideSheetCallback
import com.google.android.material.sidesheet.SideSheetDialog
import java.util.LinkedList

sealed class AdaptiveSheetBehavior {

	@JvmField
	protected val callbacks = LinkedList<AdaptiveSheetCallback>()

	abstract var state: Int

	abstract var isDraggable: Boolean

	open val isHideable: Boolean = true

	fun addCallback(callback: AdaptiveSheetCallback) {
		callbacks.add(callback)
	}

	fun removeCallback(callback: AdaptiveSheetCallback) {
		callbacks.remove(callback)
	}

	class Bottom(
		private val delegate: BottomSheetBehavior<*>,
	) : AdaptiveSheetBehavior() {

		override var state: Int
			get() = delegate.state
			set(value) {
				delegate.state = value
			}

		override var isDraggable: Boolean
			get() = delegate.isDraggable
			set(value) {
				delegate.isDraggable = value
			}

		override val isHideable: Boolean
			get() = delegate.isHideable

		var isFitToContents: Boolean
			get() = delegate.isFitToContents
			set(value) {
				delegate.isFitToContents = value
			}

		init {
			delegate.addBottomSheetCallback(
				object : BottomSheetCallback() {
					override fun onStateChanged(bottomSheet: View, newState: Int) {
						callbacks.forEach { it.onStateChanged(bottomSheet, newState) }
					}

					override fun onSlide(bottomSheet: View, slideOffset: Float) {
						callbacks.forEach { it.onSlide(bottomSheet, slideOffset) }
					}
				},
			)
		}
	}

	class Side(
		private val delegate: SideSheetBehavior<*>,
	) : AdaptiveSheetBehavior() {

		override var state: Int
			get() = delegate.state
			set(value) {
				delegate.state = value
			}

		override var isDraggable: Boolean
			get() = delegate.isDraggable
			set(value) {
				delegate.isDraggable = value
			}

		init {
			delegate.addCallback(
				object : SideSheetCallback() {
					override fun onStateChanged(sheet: View, newState: Int) {
						callbacks.forEach { it.onStateChanged(sheet, newState) }
					}

					override fun onSlide(sheet: View, slideOffset: Float) {
						callbacks.forEach { it.onSlide(sheet, slideOffset) }
					}
				},
			)
		}
	}

	companion object {

		const val STATE_EXPANDED = SideSheetBehavior.STATE_EXPANDED
		const val STATE_SETTLING = SideSheetBehavior.STATE_SETTLING
		const val STATE_DRAGGING = SideSheetBehavior.STATE_DRAGGING
		const val STATE_HIDDEN = SideSheetBehavior.STATE_HIDDEN

		fun from(dialog: Dialog?): AdaptiveSheetBehavior? = when (dialog) {
			is BottomSheetDialog -> Bottom(dialog.behavior)
			is SideSheetDialog -> Side(dialog.behavior)
			else -> null
		}

		fun from(lp: CoordinatorLayout.LayoutParams): AdaptiveSheetBehavior? = when (val behavior = lp.behavior) {
			is BottomSheetBehavior<*> -> Bottom(behavior)
			is SideSheetBehavior<*> -> Side(behavior)
			else -> null
		}
	}
}
