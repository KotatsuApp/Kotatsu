package org.koitharu.kotatsu.utils

import android.graphics.Color
import androidx.annotation.ColorInt

object DrawUtils {

	@JvmStatic
	@ColorInt
	fun invertColor(@ColorInt color: Int): Int {
		val red = Color.red(color)
		val green = Color.green(color)
		val blue = Color.blue(color)
		val alpha = Color.alpha(color)
		return Color.argb(alpha, 255 - red, 255 - green, 255 - blue)
	}
}