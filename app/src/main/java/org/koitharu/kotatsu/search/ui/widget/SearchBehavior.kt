package org.koitharu.kotatsu.search.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchBehavior(context: Context?, attrs: AttributeSet?) :
	CoordinatorLayout.Behavior<SearchToolbar>(context, attrs) {

	override fun layoutDependsOn(
		parent: CoordinatorLayout,
		child: SearchToolbar,
		dependency: View,
	): Boolean {
		return if (dependency is AppBarLayout) {
			true
		} else
			if (dependency is LinearLayout || dependency is BottomNavigationView) {
				dependency.z = child.z + 1
				true
			} else {
				super.layoutDependsOn(parent, child, dependency)
			}
	}

	override fun onDependentViewChanged(
		parent: CoordinatorLayout,
		child: SearchToolbar,
		dependency: View,
	): Boolean {
		if (dependency is AppBarLayout) {
			child.translationY = dependency.getY()
			return true
		}
		return super.onDependentViewChanged(parent, child, dependency)
	}

	override fun onStartNestedScroll(
		coordinatorLayout: CoordinatorLayout,
		child: SearchToolbar,
		directTargetChild: View,
		target: View,
		axes: Int,
		type: Int,
	): Boolean {
		return axes == ViewCompat.SCROLL_AXIS_VERTICAL
	}

}