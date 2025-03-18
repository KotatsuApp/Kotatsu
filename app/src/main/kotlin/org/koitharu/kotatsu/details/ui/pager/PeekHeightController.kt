package org.koitharu.kotatsu.details.ui.pager

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ancestors
import com.google.android.material.bottomsheet.BottomSheetBehavior

class PeekHeightController(
	private val views: Array<View>,
) : View.OnLayoutChangeListener, OnApplyWindowInsetsListener {

	private var behavior: BottomSheetBehavior<*>? = null

	fun attach() {
		behavior = findBehavior() ?: return
		views.forEach { v ->
			v.addOnLayoutChangeListener(this)
		}
		ViewCompat.setOnApplyWindowInsetsListener(views.first(), this)
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
		oldBottom: Int
	) {
		if (top != oldTop || bottom != oldBottom) {
			updatePeekHeight()
		}
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat
	): WindowInsetsCompat {
		updatePeekHeight()
		return insets
	}

	private fun updatePeekHeight() {
		behavior?.peekHeight = views.sumOf { it.height } + getBottomInset()
	}

	private fun getBottomInset(): Int = ViewCompat.getRootWindowInsets(views.first())
		?.getInsets(WindowInsetsCompat.Type.navigationBars())
		?.bottom ?: 0

	private fun findBehavior(): BottomSheetBehavior<*>? {
		return views.first().ancestors.firstNotNullOfOrNull {
			((it as? View)?.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? BottomSheetBehavior<*>
		}
	}
}
