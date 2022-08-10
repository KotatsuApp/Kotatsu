package org.koitharu.kotatsu.details.ui

import android.view.View
import android.view.View.OnLayoutChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ActionMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koitharu.kotatsu.base.ui.util.ActionModeListener
import org.koitharu.kotatsu.base.ui.widgets.BottomSheetHeaderBar

class ChaptersBottomSheetMediator(
	bottomSheet: View,
) : OnBackPressedCallback(false),
	ActionModeListener,
	BottomSheetHeaderBar.OnExpansionChangeListener,
	OnLayoutChangeListener {

	private val behavior = BottomSheetBehavior.from(bottomSheet)
	private var lockCounter = 0

	override fun handleOnBackPressed() {
		behavior.state = BottomSheetBehavior.STATE_COLLAPSED
	}

	override fun onActionModeStarted(mode: ActionMode) {
		lock()
	}

	override fun onActionModeFinished(mode: ActionMode) {
		unlock()
	}

	override fun onExpansionStateChanged(headerBar: BottomSheetHeaderBar, isExpanded: Boolean) {
		isEnabled = isExpanded
	}

	override fun onLayoutChange(
		v: View?,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int,
	) {
		val height = bottom - top
		if (height != behavior.peekHeight) {
			behavior.peekHeight = height
		}
	}

	fun lock() {
		lockCounter++
		behavior.isDraggable = lockCounter <= 0
	}

	fun unlock() {
		lockCounter--
		behavior.isDraggable = lockCounter <= 0
	}
}
