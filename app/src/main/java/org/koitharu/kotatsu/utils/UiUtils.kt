package org.koitharu.kotatsu.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import org.koitharu.kotatsu.R
import kotlin.math.roundToInt

object UiUtils {

	@JvmStatic
	@ColorInt
	fun invertColor(@ColorInt color: Int): Int {
		val red = Color.red(color)
		val green = Color.green(color)
		val blue = Color.blue(color)
		val alpha = Color.alpha(color)
		return Color.argb(alpha, 255 - red, 255 - green, 255 - blue)
	}

	@JvmStatic
	fun resolveGridSpanCount(context: Context): Int {
		val cellWidth = context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
		val screenWidth = context.resources.displayMetrics.widthPixels.toDouble()
		val estimatedCount = (screenWidth / cellWidth).roundToInt()
		return estimatedCount.coerceAtLeast(2)
	}
}