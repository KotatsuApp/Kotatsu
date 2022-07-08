package org.koitharu.kotatsu.utils.ext

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

internal fun Drawable.setCompatTint(@ColorInt color: Int) = DrawableCompat.setTint(this, color)

internal fun Drawable.wrap(): Drawable = DrawableCompat.wrap(this)