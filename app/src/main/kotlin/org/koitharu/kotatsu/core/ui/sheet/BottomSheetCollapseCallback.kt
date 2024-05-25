package org.koitharu.kotatsu.core.ui.sheet

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN

class BottomSheetCollapseCallback(
	private val sheet: ViewGroup,
	private val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(sheet),
) : OnBackPressedCallback(behavior.state == STATE_EXPANDED || behavior.state == STATE_HALF_EXPANDED) {

	init {
		behavior.addBottomSheetCallback(
			object : BottomSheetBehavior.BottomSheetCallback() {

				@SuppressLint("SwitchIntDef")
				override fun onStateChanged(view: View, state: Int) {
					when (state) {
						STATE_EXPANDED,
						STATE_HALF_EXPANDED -> isEnabled = true

						STATE_COLLAPSED,
						STATE_HIDDEN -> isEnabled = false
					}
				}

				override fun onSlide(p0: View, p1: Float) = Unit
			},
		)
	}

	override fun handleOnBackPressed() = behavior.handleBackInvoked()

	override fun handleOnBackCancelled() = behavior.cancelBackProgress()

	override fun handleOnBackProgressed(backEvent: BackEventCompat) = behavior.updateBackProgress(backEvent)

	override fun handleOnBackStarted(backEvent: BackEventCompat) = behavior.startBackProgress(backEvent)
}
