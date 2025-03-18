package org.koitharu.kotatsu.core.ui.image

import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

@Suppress("OVERRIDE_DEPRECATION")
abstract class PaintDrawable : Drawable() {

	protected abstract val paint: Paint

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun getAlpha(): Int {
		return paint.alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	override fun getColorFilter(): ColorFilter? {
		return paint.colorFilter
	}

	override fun setDither(dither: Boolean) {
		paint.isDither = dither
	}

	override fun setFilterBitmap(filter: Boolean) {
		paint.isFilterBitmap = filter
	}

	override fun isFilterBitmap(): Boolean {
		return paint.isFilterBitmap
	}

	override fun getOpacity(): Int {
		if (paint.colorFilter != null) {
			return PixelFormat.TRANSLUCENT
		}
		return when (paint.alpha) {
			0 -> PixelFormat.TRANSPARENT
			255 -> if (isOpaque()) PixelFormat.OPAQUE else PixelFormat.TRANSLUCENT
			else -> PixelFormat.TRANSLUCENT
		}
	}

	protected open fun isOpaque() = false
}
