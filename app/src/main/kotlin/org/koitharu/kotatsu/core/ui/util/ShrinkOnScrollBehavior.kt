package org.koitharu.kotatsu.core.ui.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

open class ShrinkOnScrollBehavior : Behavior<ExtendedFloatingActionButton> {

	constructor() : super()
	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

	override fun onStartNestedScroll(
		coordinatorLayout: CoordinatorLayout,
		child: ExtendedFloatingActionButton,
		directTargetChild: View,
		target: View,
		axes: Int,
		type: Int
	): Boolean {
		return axes == ViewCompat.SCROLL_AXIS_VERTICAL
	}

	override fun onNestedScroll(
		coordinatorLayout: CoordinatorLayout,
		child: ExtendedFloatingActionButton,
		target: View,
		dxConsumed: Int,
		dyConsumed: Int,
		dxUnconsumed: Int,
		dyUnconsumed: Int,
		type: Int,
		consumed: IntArray
	) {
		if (dyConsumed > 0) {
			if (child.isExtended) {
				child.shrink()
			}
		} else if (dyConsumed < 0) {
			if (!child.isExtended) {
				child.extend()
			}
		}
	}
}
