package org.koitharu.kotatsu.details.ui

import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class DetailsBottomSheetCallback(
	private val swipeRefreshLayout: SwipeRefreshLayout,
	private val navbarDimView: View,
) : BottomSheetBehavior.BottomSheetCallback() {

	override fun onStateChanged(bottomSheet: View, newState: Int) {
		swipeRefreshLayout.isEnabled = newState == BottomSheetBehavior.STATE_COLLAPSED
	}

	override fun onSlide(bottomSheet: View, slideOffset: Float) {
		navbarDimView.alpha = 1f - slideOffset.coerceAtLeast(0f)
	}
}
