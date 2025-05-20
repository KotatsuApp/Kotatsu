package org.koitharu.kotatsu.history.ui.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.image.PaintDrawable
import org.koitharu.kotatsu.core.util.ext.hasFocusStateSpecified
import org.koitharu.kotatsu.core.util.ext.scale
import org.koitharu.kotatsu.list.domain.ReadingProgress
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_NONE

class ReadingProgressDrawable(
	context: Context,
	@StyleRes styleResId: Int,
) : PaintDrawable() {

	override val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
	private val checkDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_check)
	private val lineColor: ColorStateList
	private val outlineColor: ColorStateList
	private val backgroundColor: ColorStateList
	private val textColor: ColorStateList
	private val textBounds = Rect()
	private val tempRect = Rect()
	private val desiredHeight: Int
	private val desiredWidth: Int
	private val autoFitTextSize: Boolean

	private var currentLineColor: Int = Color.TRANSPARENT
	private var currentOutlineColor: Int = Color.TRANSPARENT
	private var currentBackgroundColor: Int = Color.TRANSPARENT
	private var currentTextColor: Int = Color.TRANSPARENT
	private var hasBackground: Boolean = false
	private var hasOutline: Boolean = false
	private var hasText: Boolean = false


	var percent: Float = PROGRESS_NONE
		set(value) {
			field = value
			invalidateSelf()
		}

	var text = ""
		set(value) {
			field = value
			paint.getTextBounds(text, 0, text.length, textBounds)
			invalidateSelf()
		}

	init {
		val ta = context.obtainStyledAttributes(styleResId, R.styleable.ProgressDrawable)
		desiredHeight = ta.getDimensionPixelSize(R.styleable.ProgressDrawable_android_height, -1)
		desiredWidth = ta.getDimensionPixelSize(R.styleable.ProgressDrawable_android_width, -1)
		autoFitTextSize = ta.getBoolean(R.styleable.ProgressDrawable_autoFitTextSize, false)
		lineColor = ta.getColorStateList(R.styleable.ProgressDrawable_android_strokeColor) ?: ColorStateList.valueOf(
			Color.BLACK,
		)
		outlineColor =
			ta.getColorStateList(R.styleable.ProgressDrawable_outlineColor) ?: ColorStateList.valueOf(Color.TRANSPARENT)
		backgroundColor = ta.getColorStateList(R.styleable.ProgressDrawable_android_fillColor)?.withAlpha(
			(255 * ta.getFloat(R.styleable.ProgressDrawable_android_fillAlpha, 0f)).toInt(),
		) ?: ColorStateList.valueOf(Color.TRANSPARENT)
		textColor = ta.getColorStateList(R.styleable.ProgressDrawable_android_textColor) ?: lineColor
		paint.strokeCap = Paint.Cap.ROUND
		paint.textAlign = Paint.Align.CENTER
		paint.textSize = ta.getDimension(R.styleable.ProgressDrawable_android_textSize, paint.textSize)
		paint.strokeWidth = ta.getDimension(R.styleable.ProgressDrawable_strokeWidth, 1f)
		ta.recycle()
		checkDrawable?.setTintList(textColor)
		onStateChange(state)
	}

	override fun onBoundsChange(bounds: Rect) {
		super.onBoundsChange(bounds)
		if (autoFitTextSize) {
			val innerWidth = bounds.width() - (paint.strokeWidth * 2f)
			paint.textSize = getTextSizeForWidth(innerWidth, "100%")
			paint.getTextBounds(text, 0, text.length, textBounds)
			invalidateSelf()
		}
	}

	override fun draw(canvas: Canvas) {
		if (percent < 0f) {
			return
		}
		val cx = bounds.exactCenterX()
		val cy = bounds.exactCenterY()
		val radius = minOf(bounds.width(), bounds.height()) / 2f
		if (hasBackground) {
			paint.style = Paint.Style.FILL
			paint.color = currentBackgroundColor
			canvas.drawCircle(cx, cy, radius, paint)
		}
		val innerRadius = radius - paint.strokeWidth / 2f
		paint.style = Paint.Style.STROKE
		if (hasOutline) {
			paint.color = currentOutlineColor
			canvas.drawCircle(cx, cy, innerRadius, paint)
		}
		paint.color = currentLineColor
		canvas.drawArc(
			cx - innerRadius,
			cy - innerRadius,
			cx + innerRadius,
			cy + innerRadius,
			-90f,
			360f * percent,
			false,
			paint,
		)
		if (hasText) {
			if (checkDrawable != null && ReadingProgress.isCompleted(percent)) {
				tempRect.set(bounds)
				tempRect.scale(0.6)
				checkDrawable.bounds = tempRect
				checkDrawable.draw(canvas)
			} else {
				paint.style = Paint.Style.FILL
				paint.color = currentTextColor
				val ty = bounds.height() / 2f + textBounds.height() / 2f - textBounds.bottom
				canvas.drawText(text, cx, ty, paint)
			}
		}
	}

	override fun getIntrinsicHeight() = desiredHeight

	override fun getIntrinsicWidth() = desiredWidth

	override fun isStateful(): Boolean = lineColor.isStateful ||
		outlineColor.isStateful ||
		backgroundColor.isStateful ||
		textColor.isStateful ||
		checkDrawable?.isStateful == true

	@RequiresApi(Build.VERSION_CODES.S)
	override fun hasFocusStateSpecified(): Boolean = lineColor.hasFocusStateSpecified() ||
		outlineColor.hasFocusStateSpecified() ||
		backgroundColor.hasFocusStateSpecified() ||
		textColor.hasFocusStateSpecified() ||
		checkDrawable?.hasFocusStateSpecified() == true

	override fun onStateChange(state: IntArray): Boolean {
		val prevLineColor = currentLineColor
		currentLineColor = lineColor.getColorForState(state, lineColor.defaultColor)
		val prevOutlineColor = currentOutlineColor
		currentOutlineColor = outlineColor.getColorForState(state, outlineColor.defaultColor)
		val prevBackgroundColor = currentBackgroundColor
		currentBackgroundColor = backgroundColor.getColorForState(state, backgroundColor.defaultColor)
		val prevTextColor = currentTextColor
		currentTextColor = textColor.getColorForState(state, textColor.defaultColor)
		hasBackground = Color.alpha(currentBackgroundColor) != 0
		hasOutline = Color.alpha(currentOutlineColor) != 0
		hasText = Color.alpha(currentTextColor) != 0 && paint.textSize > 0
		return checkDrawable?.setState(state) == true ||
			prevLineColor != currentLineColor ||
			prevOutlineColor != currentOutlineColor ||
			prevBackgroundColor != currentBackgroundColor ||
			prevTextColor != currentTextColor
	}

	private fun getTextSizeForWidth(width: Float, text: String): Float {
		val testTextSize = 48f
		paint.textSize = testTextSize
		paint.getTextBounds(text, 0, text.length, tempRect)
		return testTextSize * width / tempRect.width()
	}
}
