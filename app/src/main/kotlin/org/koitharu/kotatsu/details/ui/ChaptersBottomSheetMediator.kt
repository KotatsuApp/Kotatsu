package org.koitharu.kotatsu.details.ui

import android.view.View
import android.view.View.OnLayoutChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ActionMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.koitharu.kotatsu.core.ui.util.ActionModeListener
import org.koitharu.kotatsu.core.util.ext.doOnExpansionsChanged

class ChaptersBottomSheetMediator(
	private val behavior: BottomSheetBehavior<*>,
) : OnBackPressedCallback(false),
	ActionModeListener,
	OnLayoutChangeListener {

	private var lockCounter = 0

	init {
		behavior.doOnExpansionsChanged { isExpanded ->
			isEnabled = isExpanded
			if (!isExpanded) {
				unlock()
			}
		}
	}

	override fun handleOnBackPressed() {
		behavior.state = BottomSheetBehavior.STATE_COLLAPSED
	}

	override fun onActionModeStarted(mode: ActionMode) {
		behavior.state = BottomSheetBehavior.STATE_EXPANDED
		lock()
	}

	override fun onActionModeFinished(mode: ActionMode) {
		unlock()
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
		if (lockCounter < 0) {
			lockCounter = 0
		}
		behavior.isDraggable = lockCounter <= 0
	}
}
