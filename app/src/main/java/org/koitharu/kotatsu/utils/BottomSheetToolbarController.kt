package org.koitharu.kotatsu.utils

import android.view.View
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.R as materialR

open class BottomSheetToolbarController(
	protected val toolbar: Toolbar,
) : BottomSheetBehavior.BottomSheetCallback() {

	override fun onStateChanged(bottomSheet: View, newState: Int) {
		if (newState == BottomSheetBehavior.STATE_EXPANDED) {
			toolbar.setNavigationIcon(materialR.drawable.abc_ic_clear_material)
		} else {
			toolbar.navigationIcon = null
		}
	}

	override fun onSlide(bottomSheet: View, slideOffset: Float) {

	}
}