package org.koitharu.kotatsu.core.ui.util

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomSheetNoHalfExpandedCallback() : BottomSheetBehavior.BottomSheetCallback() {

	private var previousStableState = BottomSheetBehavior.STATE_COLLAPSED

	override fun onStateChanged(sheet: View, state: Int) {
		if (state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
			val behavior = (sheet.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? BottomSheetBehavior<*>
			behavior?.state = previousStableState
		} else if (state == BottomSheetBehavior.STATE_EXPANDED || state == BottomSheetBehavior.STATE_COLLAPSED) {
			previousStableState = state
		}
	}

	override fun onSlide(sheet: View, offset: Float) = Unit
}
