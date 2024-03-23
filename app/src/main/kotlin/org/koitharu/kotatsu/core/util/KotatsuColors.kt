package org.koitharu.kotatsu.core.util

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.parsers.model.Manga
import kotlin.math.absoluteValue

object KotatsuColors {

	@ColorInt
	fun segmentColor(context: Context, @AttrRes resId: Int): Int {
		val colorHex = String.format("%06x", context.getThemeColor(resId))
		val hue = getHue(colorHex)
		val color = ColorUtils.HSLToColor(floatArrayOf(hue, 0.5f, 0.5f))
		val backgroundColor = context.getThemeColor(R.attr.colorSurfaceContainerHigh)
		return MaterialColors.harmonize(color, backgroundColor)
	}

	@ColorInt
	fun random(seed: Any): Int {
		val hue = (seed.hashCode() % 360).absoluteValue.toFloat()
		return ColorUtils.HSLToColor(floatArrayOf(hue, 0.5f, 0.5f))
	}

	@ColorInt
	fun ofManga(context: Context, manga: Manga?): Int {
		val color = if (manga != null) {
			val hue = (manga.id.absoluteValue % 360).toFloat()
			ColorUtils.HSLToColor(floatArrayOf(hue, 0.5f, 0.5f))
		} else {
			context.getThemeColor(R.attr.colorOutline)
		}
		val backgroundColor = context.getThemeColor(R.attr.colorSurfaceContainerHigh)
		return MaterialColors.harmonize(color, backgroundColor)
	}

	private fun getHue(hex: String): Float {
		val r = (hex.substring(0, 2).toInt(16)).toFloat()
		val g = (hex.substring(2, 4).toInt(16)).toFloat()
		val b = (hex.substring(4, 6).toInt(16)).toFloat()

		var hue = 0F
		if ((r >= g) && (g >= b)) {
			hue = 60 * (g - b) / (r - b)
		} else if ((g > r) && (r >= b)) {
			hue = 60 * (2 - (r - b) / (g - b))
		}
		return hue
	}
}
