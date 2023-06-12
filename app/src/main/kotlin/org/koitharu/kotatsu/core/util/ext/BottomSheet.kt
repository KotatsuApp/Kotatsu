package org.koitharu.kotatsu.core.util.ext

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

fun BottomSheetBehavior<*>.doOnExpansionsChanged(callback: (isExpanded: Boolean) -> Unit) {
	var isExpended = state == BottomSheetBehavior.STATE_EXPANDED
	callback(isExpended)
	addBottomSheetCallback(
		object : BottomSheetCallback() {
			override fun onStateChanged(bottomSheet: View, newState: Int) {
				val expanded = newState == BottomSheetBehavior.STATE_EXPANDED
				if (expanded != isExpended) {
					isExpended = expanded
					callback(expanded)
				}
			}

			override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
		},
	)
}
