package org.koitharu.kotatsu.details.ui

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ActionMode
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import org.koitharu.kotatsu.core.ui.util.ActionModeListener
import org.koitharu.kotatsu.core.util.ext.doOnExpansionsChanged
import org.koitharu.kotatsu.core.util.ext.setTabsEnabled

class ChaptersBottomSheetMediator(
	private val behavior: BottomSheetBehavior<*>,
	private val pager: ViewPager2,
	private val tabLayout: TabLayout,
) : OnBackPressedCallback(false),
	ActionModeListener,
	OnLayoutChangeListener, View.OnGenericMotionListener {

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

	override fun onGenericMotion(v: View?, event: MotionEvent): Boolean {
		if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
			if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
				if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0f) {
					behavior.state = BottomSheetBehavior.STATE_COLLAPSED
				} else {
					behavior.state = BottomSheetBehavior.STATE_EXPANDED
				}
				return true
			}
		}
		return false
	}

	fun lock() {
		lockCounter++
		updateLock()
	}

	fun unlock() {
		lockCounter--
		if (lockCounter < 0) {
			lockCounter = 0
		}
		updateLock()
	}

	private fun updateLock() {
		behavior.isDraggable = lockCounter <= 0
		pager.isUserInputEnabled = lockCounter <= 0
		tabLayout.setTabsEnabled(lockCounter <= 0)
	}
}
