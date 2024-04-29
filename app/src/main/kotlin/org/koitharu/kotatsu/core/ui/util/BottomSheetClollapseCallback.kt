package org.koitharu.kotatsu.core.ui.util

import android.view.View
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import org.koitharu.kotatsu.core.util.ext.doOnExpansionsChanged

class BottomSheetClollapseCallback(
	private val behavior: BottomSheetBehavior<*>,
) : OnBackPressedCallback(behavior.state == STATE_EXPANDED) {

	init {
		behavior.addBottomSheetCallback(
			object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(view: View, state: Int) {
					isEnabled = state == STATE_EXPANDED || state == STATE_HALF_EXPANDED
				}

				override fun onSlide(p0: View, p1: Float) = Unit
			},
		)
	}

	override fun handleOnBackPressed() {
		behavior.state = STATE_COLLAPSED
	}
}
