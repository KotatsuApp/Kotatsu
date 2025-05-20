package org.koitharu.kotatsu.core.util.ext

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.roundToInt

fun Rect.scale(factor: Double) {
	val newWidth = (width() * factor).roundToInt()
	val newHeight = (height() * factor).roundToInt()
	inset(
		(width() - newWidth) / 2,
		(height() - newHeight) / 2,
	)
}

inline fun <R> Bitmap.use(block: (Bitmap) -> R) = try {
	block(this)
} finally {
	recycle()
}

fun ColorStateList.hasFocusStateSpecified(): Boolean {
	return getColorForState(intArrayOf(android.R.attr.state_focused), defaultColor) != defaultColor
}
