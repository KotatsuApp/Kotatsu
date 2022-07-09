package org.koitharu.kotatsu.utils.ext

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