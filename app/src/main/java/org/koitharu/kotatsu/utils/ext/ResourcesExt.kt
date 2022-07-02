package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.roundToInt

@Px
fun Resources.resolveDp(dp: Int) = (dp * displayMetrics.density).roundToInt()

@Px
fun Resources.resolveDp(dp: Float) = dp * displayMetrics.density

@ColorInt
fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
	val typedArray = obtainStyledAttributes(intArrayOf(resource))
	val color = typedArray.getColor(0, 0)
	typedArray.recycle()

	if (alphaFactor < 1f) {
		val alpha = (color.alpha * alphaFactor).roundToInt()
		return Color.argb(alpha, color.red, color.green, color.blue)
	}

	return color
}