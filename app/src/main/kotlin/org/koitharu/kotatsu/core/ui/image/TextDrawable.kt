package org.koitharu.kotatsu.core.ui.image

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.core.graphics.PaintCompat

class TextDrawable(
	val text: String,
) : Drawable() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
	private val textBounds = Rect()
	private val textPoint = PointF()

	var textSize: Float
		get() = paint.textSize
		set(value) {
			paint.textSize = value
			measureTextBounds()
		}

	var textColor: ColorStateList = ColorStateList.valueOf(Color.BLACK)
		set(value) {
			field = value
			onStateChange(state)
		}

	init {
		measureTextBounds()
	}

	override fun draw(canvas: Canvas) {
		canvas.drawText(text, textPoint.x, textPoint.y, paint)
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.setColorFilter(colorFilter)
	}

	override fun getOpacity(): Int = when (paint.alpha) {
		0 -> PixelFormat.TRANSPARENT
		255 -> PixelFormat.OPAQUE
		else -> PixelFormat.TRANSLUCENT
	}

	override fun onBoundsChange(bounds: Rect) {
		textPoint.set(
			bounds.exactCenterX() - textBounds.exactCenterX(),
			bounds.exactCenterY() - textBounds.exactCenterY(),
		)
	}

	override fun getIntrinsicWidth(): Int = textBounds.width()

	override fun getIntrinsicHeight(): Int = textBounds.height()

	override fun setDither(dither: Boolean) {
		paint.isDither = dither
	}

	override fun isStateful(): Boolean = textColor.isStateful

	override fun hasFocusStateSpecified(): Boolean = textColor.getColorForState(
		intArrayOf(android.R.attr.state_focused),
		textColor.defaultColor,
	) != textColor.defaultColor

	override fun onStateChange(state: IntArray): Boolean {
		val prevColor = paint.color
		paint.color = textColor.getColorForState(state, textColor.defaultColor)
		return paint.color != prevColor
	}

	private fun measureTextBounds() {
		paint.getTextBounds(text, 0, text.length, textBounds)
		onBoundsChange(bounds)
	}

	companion object {

		fun compound(textView: TextView, text: String): TextDrawable? {
			val drawable = TextDrawable(text)
			drawable.textSize = textView.textSize
			drawable.textColor = textView.textColors
			return drawable.takeIf {
				PaintCompat.hasGlyph(drawable.paint, text)
			}
		}
	}
}
