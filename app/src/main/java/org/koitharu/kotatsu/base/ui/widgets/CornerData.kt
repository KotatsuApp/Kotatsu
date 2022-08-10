package org.koitharu.kotatsu.base.ui.widgets

import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.shape.AbsoluteCornerSize
import com.google.android.material.shape.CornerSize

class CornerData(
	var topLeft: CornerSize,
	var bottomLeft: CornerSize,
	var topRight: CornerSize,
	var bottomRight: CornerSize,
) {

	fun start(view: View): CornerData {
		return if (isLayoutRtl(view)) right() else left()
	}

	fun end(view: View): CornerData {
		return if (isLayoutRtl(view)) left() else right()
	}

	fun left(): CornerData {
		return CornerData(topLeft, bottomLeft, noCorner, noCorner)
	}

	fun right(): CornerData {
		return CornerData(noCorner, noCorner, topRight, bottomRight)
	}

	fun top(): CornerData {
		return CornerData(topLeft, noCorner, topRight, noCorner)
	}

	fun bottom(): CornerData {
		return CornerData(noCorner, bottomLeft, noCorner, bottomRight)
	}

	private companion object {

		val noCorner: CornerSize = AbsoluteCornerSize(0f)

		fun isLayoutRtl(view: View): Boolean {
			return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL
		}
	}
}
