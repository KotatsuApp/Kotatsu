package org.koitharu.kotatsu.history.ui.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.StyleRes
import androidx.core.graphics.ColorUtils
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import kotlin.math.roundToInt

class ReadingProgressDrawable(
	context: Context,
	@StyleRes styleResId: Int,
) : Drawable() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val lineColor: Int
	private val outlineColor: Int
	private val backgroundColor: Int
	private val textColor: Int
	private val textPattern = context.getString(R.string.percent_string_pattern)
	private val textBounds = Rect()
	private val hasBackground: Boolean
	private val hasOutline: Boolean
	private val hasText: Boolean
	private val desiredHeight: Int
	private val desiredWidth: Int
	private val autoFitTextSize: Boolean

	var progress: Float = PROGRESS_NONE
		set(value) {
			field = value
			text = textPattern.format((value * 100f).roundToInt().toString())
			paint.getTextBounds(text, 0, text.length, textBounds)
			invalidateSelf()
		}
	private var text = ""

	init {
		val ta = context.obtainStyledAttributes(styleResId, R.styleable.ProgressDrawable)
		desiredHeight = ta.getDimensionPixelSize(R.styleable.ProgressDrawable_android_height, -1)
		desiredWidth = ta.getDimensionPixelSize(R.styleable.ProgressDrawable_android_width, -1)
		autoFitTextSize = ta.getBoolean(R.styleable.ProgressDrawable_autoFitTextSize, false)
		lineColor = ta.getColor(R.styleable.ProgressDrawable_android_strokeColor, Color.BLACK)
		outlineColor = ta.getColor(R.styleable.ProgressDrawable_outlineColor, Color.TRANSPARENT)
		backgroundColor = ColorUtils.setAlphaComponent(
			ta.getColor(R.styleable.ProgressDrawable_android_fillColor, Color.TRANSPARENT),
			(255 * ta.getFloat(R.styleable.ProgressDrawable_android_fillAlpha, 0f)).toInt(),
		)
		textColor = ta.getColor(R.styleable.ProgressDrawable_android_textColor, lineColor)
		paint.strokeCap = Paint.Cap.ROUND
		paint.textAlign = Paint.Align.CENTER
		paint.textSize = ta.getDimension(R.styleable.ProgressDrawable_android_textSize, paint.textSize)
		paint.strokeWidth = ta.getDimension(R.styleable.ProgressDrawable_strokeWidth, 1f)
		hasBackground = Color.alpha(backgroundColor) != 0
		hasOutline = Color.alpha(outlineColor) != 0
		hasText = Color.alpha(textColor) != 0 && paint.textSize > 0
		ta.recycle()
	}

	override fun onBoundsChange(bounds: Rect) {
		super.onBoundsChange(bounds)
		if (autoFitTextSize) {
			val innerWidth = bounds.width() - (paint.strokeWidth * 2f)
			paint.textSize = getTextSizeForWidth(innerWidth, "100%")
		}
	}

	override fun draw(canvas: Canvas) {
		if (progress < 0f) {
			return
		}
		val cx = bounds.exactCenterX()
		val cy = bounds.exactCenterY()
		val radius = minOf(bounds.width(), bounds.height()) / 2f
		if (hasBackground) {
			paint.style = Paint.Style.FILL
			paint.color = backgroundColor
			canvas.drawCircle(cx, cy, radius, paint)
		}
		val innerRadius = radius - paint.strokeWidth / 2f
		paint.style = Paint.Style.STROKE
		if (hasOutline) {
			paint.color = outlineColor
			canvas.drawCircle(cx, cy, innerRadius, paint)
		}
		paint.color = lineColor
		canvas.drawArc(
			cx - innerRadius,
			cy - innerRadius,
			cx + innerRadius,
			cy + innerRadius,
			-90f,
			360f * progress,
			false,
			paint,
		)
		if (hasText) {
			paint.style = Paint.Style.FILL
			paint.color = textColor
			val ty = bounds.height() / 2f + textBounds.height() / 2f - textBounds.bottom
			canvas.drawText(text, cx, ty, paint)
		}
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("Deprecated in Java")
	override fun getOpacity() = PixelFormat.TRANSLUCENT

	override fun getIntrinsicHeight() = desiredHeight

	override fun getIntrinsicWidth() = desiredWidth

	private fun getTextSizeForWidth(width: Float, text: String): Float {
		val testTextSize = 48f
		paint.textSize = testTextSize
		paint.getTextBounds(text, 0, text.length, textBounds)
		return testTextSize * width / textBounds.width()
	}
}