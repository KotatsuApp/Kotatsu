package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils

val Resources.isNightMode: Boolean
	get() = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

fun Context.getThemeDrawable(
	@AttrRes resId: Int,
) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getDrawable(0)
}

@ColorInt
fun Context.getThemeColor(
	@AttrRes resId: Int,
	@ColorInt fallback: Int = Color.TRANSPARENT,
) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getColor(0, fallback)
}

@Px
fun Context.getThemeDimensionPixelSize(
	@AttrRes resId: Int,
	@Px fallback: Int = 0,
) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getDimensionPixelSize(0, fallback)
}

@Px
fun Context.getThemeDimensionPixelOffset(
	@AttrRes resId: Int,
	@Px fallback: Int = 0,
) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getDimensionPixelOffset(0, fallback)
}

@ColorInt
fun Context.getThemeColor(
	@AttrRes resId: Int,
	@FloatRange(from = 0.0, to = 1.0) alphaFactor: Float,
	@ColorInt fallback: Int = Color.TRANSPARENT,
): Int {
	if (alphaFactor <= 0f) {
		return Color.TRANSPARENT
	}
	val color = getThemeColor(resId, fallback)
	if (alphaFactor >= 1f) {
		return color
	}
	return ColorUtils.setAlphaComponent(color, (0xFF * alphaFactor).toInt())
}

fun Context.getThemeColorStateList(
	@AttrRes resId: Int,
) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getColorStateList(0)
}

fun Context.getThemeResId(
	@AttrRes resId: Int,
	fallback: Int
): Int = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getResourceId(0, fallback)
}

@Deprecated("")
fun TypedArray.getDrawableCompat(context: Context, index: Int): Drawable? {
	val resId = getResourceId(index, 0)
	return if (resId != 0) ContextCompat.getDrawable(context, resId) else null
}
