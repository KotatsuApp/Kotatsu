package org.koitharu.kotatsu.core.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.measureHeight

class HideBottomNavigationOnScrollBehavior @JvmOverloads constructor(
	context: Context? = null,
	attrs: AttributeSet? = null,
) : CoordinatorLayout.Behavior<BottomNavigationView>(context, attrs) {

	@ViewCompat.NestedScrollType
	private var lastStartedType: Int = 0

	private var offsetAnimator: ValueAnimator? = null

	private var dyRatio = 1F

	var isPinned: Boolean = false
		set(value) {
			field = value
			if (value) {
				offsetAnimator?.cancel()
				offsetAnimator = null
			}
		}

	override fun layoutDependsOn(parent: CoordinatorLayout, child: BottomNavigationView, dependency: View): Boolean {
		return dependency is AppBarLayout
	}

	override fun onDependentViewChanged(
		parent: CoordinatorLayout,
		child: BottomNavigationView,
		dependency: View,
	): Boolean {
		val appBarSize = dependency.measureHeight()
		dyRatio = if (appBarSize > 0) {
			child.measureHeight().toFloat() / appBarSize
		} else {
			1F
		}
		return false
	}

	override fun onStartNestedScroll(
		coordinatorLayout: CoordinatorLayout,
		child: BottomNavigationView,
		directTargetChild: View,
		target: View,
		axes: Int,
		type: Int,
	): Boolean {
		if (isPinned || axes != ViewCompat.SCROLL_AXIS_VERTICAL) {
			return false
		}
		lastStartedType = type
		offsetAnimator?.cancel()
		return true
	}

	override fun onNestedPreScroll(
		coordinatorLayout: CoordinatorLayout,
		child: BottomNavigationView,
		target: View,
		dx: Int,
		dy: Int,
		consumed: IntArray,
		type: Int,
	) {
		super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
		if (!isPinned) {
			child.translationY = (child.translationY + (dy * dyRatio)).coerceIn(0F, child.height.toFloat())
		}
	}

	override fun onStopNestedScroll(
		coordinatorLayout: CoordinatorLayout,
		child: BottomNavigationView,
		target: View,
		type: Int,
	) {
		if (!isPinned && (lastStartedType == ViewCompat.TYPE_TOUCH || type == ViewCompat.TYPE_NON_TOUCH)) {
			animateBottomNavigationVisibility(child, child.translationY < child.height / 2)
		}
	}

	private fun animateBottomNavigationVisibility(child: BottomNavigationView, isVisible: Boolean) {
		offsetAnimator?.cancel()
		offsetAnimator = ValueAnimator().apply {
			interpolator = DecelerateInterpolator()
			duration = child.context.getAnimationDuration(R.integer.config_shorterAnimTime)
			addUpdateListener {
				child.translationY = it.animatedValue as Float
			}
		}
		offsetAnimator?.setFloatValues(
			child.translationY,
			if (isVisible) 0F else child.height.toFloat(),
		)
		offsetAnimator?.start()
	}
}
