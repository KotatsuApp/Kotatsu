package org.koitharu.kotatsu.core.ui.util

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ancestors
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class PagerNestedScrollHelper(
	private val recyclerView: RecyclerView,
) : DefaultLifecycleObserver {

	fun bind(lifecycleOwner: LifecycleOwner) {
		lifecycleOwner.lifecycle.addObserver(this)
		recyclerView.isNestedScrollingEnabled = lifecycleOwner.lifecycle.currentState.isAtLeast(RESUMED)
	}

	override fun onPause(owner: LifecycleOwner) {
		recyclerView.isNestedScrollingEnabled = false
		invalidateBottomSheetScrollTarget()
	}

	override fun onResume(owner: LifecycleOwner) {
		recyclerView.isNestedScrollingEnabled = true
	}

	override fun onDestroy(owner: LifecycleOwner) {
		owner.lifecycle.removeObserver(this)
	}

	/**
	 * Here we need to invalidate the `nestedScrollingChildRef` of the [BottomSheetBehavior]
	 */
	private fun invalidateBottomSheetScrollTarget() {
		var handleCoordinator = false
		for (parent in recyclerView.ancestors) {
			if (handleCoordinator && parent is CoordinatorLayout) {
				parent.requestLayout()
				break
			}
			val lp = (parent as? View)?.layoutParams ?: continue
			if (lp is CoordinatorLayout.LayoutParams && lp.behavior is BottomSheetBehavior<*>) {
				handleCoordinator = true
			}
		}
	}
}
