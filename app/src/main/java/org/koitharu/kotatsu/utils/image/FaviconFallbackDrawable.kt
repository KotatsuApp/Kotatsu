package org.koitharu.kotatsu.utils.image

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import kotlin.math.absoluteValue

class FaviconFallbackDrawable(
	context: Context,
	name: String,
) : Drawable() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val letter = name.take(1).uppercase()
	private val color = colorOfString(name)
	private val textBounds = Rect()
	private val tempRect = Rect()

	init {
		paint.style = Paint.Style.FILL
		paint.textAlign = Paint.Align.CENTER
		paint.isFakeBoldText = true
	}

	override fun draw(canvas: Canvas) {
		val cx = bounds.exactCenterX()
		paint.color = color
		canvas.drawPaint(paint)
		paint.color = Color.WHITE
		val ty = bounds.height() / 2f + textBounds.height() / 2f - textBounds.bottom
		canvas.drawText(letter, cx, ty, paint)
	}

	override fun onBoundsChange(bounds: Rect) {
		super.onBoundsChange(bounds)
		val innerWidth = bounds.width() - (paint.strokeWidth * 2f)
		paint.textSize = getTextSizeForWidth(innerWidth, "100%")
		paint.getTextBounds(letter, 0, letter.length, textBounds)
		invalidateSelf()
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("Deprecated in Java")
	override fun getOpacity() = PixelFormat.TRANSPARENT

	private fun getTextSizeForWidth(width: Float, text: String): Float {
		val testTextSize = 48f
		paint.textSize = testTextSize
		paint.getTextBounds(text, 0, text.length, tempRect)
		return testTextSize * width / tempRect.width()
	}

	private fun colorOfString(str: String): Int {
		val hue = (str.hashCode() % 360).absoluteValue.toFloat()
		return ColorUtils.HSLToColor(floatArrayOf(hue, 0.5f, 0.5f))
	}
}