package org.koitharu.kotatsu.core.ui.image

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.PaintCompat
import org.koitharu.kotatsu.core.util.ext.hasFocusStateSpecified

class TextDrawable(
	val text: String,
) : PaintDrawable() {

	override val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
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

	override fun onBoundsChange(bounds: Rect) {
		textPoint.set(
			bounds.exactCenterX() - textBounds.exactCenterX(),
			bounds.exactCenterY() - textBounds.exactCenterY(),
		)
	}

	override fun getIntrinsicWidth(): Int = textBounds.width()

	override fun getIntrinsicHeight(): Int = textBounds.height()

	override fun isStateful(): Boolean = textColor.isStateful

	@RequiresApi(Build.VERSION_CODES.S)
	override fun hasFocusStateSpecified(): Boolean = textColor.hasFocusStateSpecified()

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
