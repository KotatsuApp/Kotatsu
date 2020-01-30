package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.content.res.use

@Px
fun Context.getThemeDimen(@AttrRes resId: Int) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getDimension(0, 0f)
}

fun Context.getThemeDrawable(@AttrRes resId: Int) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getDrawable(0)
}

@ColorInt
fun Context.getThemeColor(@AttrRes resId: Int, @ColorInt default: Int = Color.TRANSPARENT) = obtainStyledAttributes(intArrayOf(resId)).use {
	it.getColor(0, default)
}