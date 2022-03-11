package org.koitharu.kotatsu.utils.ext

import android.view.View
import androidx.core.graphics.Insets

fun Insets.getStart(view: View): Int {
	return if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
		right
	} else {
		left
	}
}

fun Insets.getEnd(view: View): Int {
	return if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
		left
	} else {
		right
	}
}