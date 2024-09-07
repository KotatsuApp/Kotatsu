package org.koitharu.kotatsu.reader.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.util.AttributeSet
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withScale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.measureDimension
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.google.android.material.R as materialR


class ReaderInfoBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textBounds = Rect()
	private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
	private val systemStateReceiver = SystemStateReceiver()
	private var insetLeft: Int = 0
	private var insetRight: Int = 0
	private var insetTop: Int = 0
	private val insetLeftFallback: Int
	private val insetRightFallback: Int
	private val insetTopFallback: Int
	private val insetCornerFallback = getSystemUiDimensionOffset("rounded_corner_content_padding")
	private val colorText = ColorUtils.setAlphaComponent(
		context.getThemeColor(materialR.attr.colorOnSurface, Color.BLACK),
		200,
	)
	private val colorOutline = ColorUtils.setAlphaComponent(
		context.getThemeColor(materialR.attr.colorSurface, Color.WHITE),
		200,
	)
	private val batteryIcon = ContextCompat.getDrawable(context, R.drawable.ic_battery_outline)

	private var timeText = timeFormat.format(LocalTime.now())
	private var batteryText = ""
	private var text: String = ""
	private var prevTextHeight: Int = 0

	private val innerHeight
		get() = height - paddingTop - paddingBottom - insetTop

	private val innerWidth
		get() = width - paddingLeft - paddingRight - insetLeft - insetRight

	var isTimeVisible: Boolean = true
		set(value) {
			field = value
			invalidate()
		}

	init {
		context.withStyledAttributes(attrs, R.styleable.ReaderInfoBarView, defStyleAttr) {
			paint.strokeWidth = getDimension(R.styleable.ReaderInfoBarView_android_strokeWidth, 2f)
			paint.textSize = getDimension(R.styleable.ReaderInfoBarView_android_textSize, 16f)
		}
		val insetStart = getSystemUiDimensionOffset("status_bar_padding_start").coerceAtLeast(0)
		val insetEnd = getSystemUiDimensionOffset("status_bar_padding_end").coerceAtLeast(0)
		val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
		insetLeftFallback = if (isRtl) insetEnd else insetStart
		insetRightFallback = if (isRtl) insetStart else insetEnd
		insetTopFallback = minOf(insetLeftFallback, insetRightFallback)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight + insetLeft + insetRight
		val desiredHeight = maxOf(
			computeTextHeight().also { prevTextHeight = it },
			suggestedMinimumHeight,
		) + paddingTop + paddingBottom + insetTop
		setMeasuredDimension(
			measureDimension(desiredWidth, widthMeasureSpec),
			measureDimension(desiredHeight, heightMeasureSpec),
		)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		computeTextHeight()
		val h = innerHeight.toFloat()
		val ty = h / 2f + textBounds.height() / 2f - textBounds.bottom
		paint.textAlign = Paint.Align.LEFT
		canvas.drawTextOutline(
			text,
			(paddingLeft + insetLeft).toFloat(),
			paddingTop + insetTop + ty,
		)
		if (isTimeVisible) {
			paint.textAlign = Paint.Align.RIGHT
			var endX = (width - paddingRight - insetRight).toFloat()
			canvas.drawTextOutline(timeText, endX, paddingTop + insetTop + ty)
			if (batteryText.isNotEmpty()) {
				paint.getTextBounds(timeText, 0, timeText.length, textBounds)
				endX -= textBounds.width()
				endX -= h * 0.6f
				canvas.drawTextOutline(batteryText, endX, paddingTop + insetTop + ty)
				batteryIcon?.let {
					paint.getTextBounds(batteryText, 0, batteryText.length, textBounds)
					endX -= textBounds.width()
					val iconCenter = paddingTop + insetTop + textBounds.height() / 2
					it.setBounds(
						(endX - h).toInt(),
						(iconCenter - h / 2).toInt(),
						endX.toInt(),
						(iconCenter + h / 2).toInt(),
					)
					it.drawWithOutline(canvas)
				}
			}
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		updateCutoutInsets(ViewCompat.getRootWindowInsets(this))
	}

	override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
		updateCutoutInsets(WindowInsetsCompat.toWindowInsetsCompat(insets))
		return super.onApplyWindowInsets(insets)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		ContextCompat.registerReceiver(
			context,
			systemStateReceiver,
			IntentFilter().apply {
				addAction(Intent.ACTION_TIME_TICK)
				addAction(Intent.ACTION_BATTERY_CHANGED)
			},
			ContextCompat.RECEIVER_EXPORTED,
		)
		updateCutoutInsets(ViewCompat.getRootWindowInsets(this))
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		context.unregisterReceiver(systemStateReceiver)
	}

	fun update(state: ReaderUiState?) {
		text = if (state != null) {
			context.getString(
				R.string.reader_info_pattern,
				state.chapterNumber,
				state.chaptersTotal,
				state.currentPage + 1,
				state.totalPages,
			) + if (state.percent in 0f..1f) {
				"     " + context.getString(R.string.percent_string_pattern, (state.percent * 100).format())
			} else {
				""
			}
		} else {
			""
		}
		val newHeight = computeTextHeight()
		if (newHeight != prevTextHeight) {
			prevTextHeight = newHeight
			requestLayout()
		}
		invalidate()
	}

	private fun computeTextHeight(): Int {
		val str = text + batteryText + timeText
		paint.getTextBounds(str, 0, str.length, textBounds)
		return textBounds.height()
	}

	private fun Canvas.drawTextOutline(text: String, x: Float, y: Float) {
		paint.color = colorOutline
		paint.style = Paint.Style.STROKE
		drawText(text, x, y, paint)
		paint.color = colorText
		paint.style = Paint.Style.FILL
		drawText(text, x, y, paint)
	}

	private fun Drawable.drawWithOutline(canvas: Canvas) {
		var requiredScale = (bounds.width() + paint.strokeWidth * 2f) / bounds.width().toFloat()
		setTint(colorOutline)
		canvas.withScale(requiredScale, requiredScale, bounds.exactCenterX(), bounds.exactCenterY()) {
			draw(canvas)
		}
		requiredScale = 1f / requiredScale
		canvas.withScale(requiredScale, requiredScale, bounds.exactCenterX(), bounds.exactCenterY()) {
			draw(canvas)
		}
		setTint(colorText)
		draw(canvas)
	}

	private fun updateCutoutInsets(insetsCompat: WindowInsetsCompat?) {
		insetLeft = insetLeftFallback
		insetRight = insetRightFallback
		insetTop = insetTopFallback
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && insetsCompat != null) {
			val nativeInsets = insetsCompat.toWindowInsets()
			nativeInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.let { corner ->
				insetLeft += corner.radius
			}
			nativeInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.let { corner ->
				insetRight += corner.radius
			}
		} else {
			insetLeft += insetCornerFallback
			insetRight += insetCornerFallback
		}
		insetsCompat?.displayCutout?.let { cutout ->
			for (rect in cutout.boundingRects) {
				if (rect.left <= paddingLeft) {
					insetLeft += rect.width()
				}
				if (rect.right >= width - paddingRight) {
					insetRight += rect.width()
				}
			}
		}
	}

	private inner class SystemStateReceiver : BroadcastReceiver() {

		override fun onReceive(context: Context, intent: Intent) {
			val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
			val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
			if (level != -1 && scale != -1) {
				batteryText = context.getString(R.string.percent_string_pattern, (level * 100 / scale).toString())
			}

			timeText = timeFormat.format(LocalTime.now())
			if (isTimeVisible) {
				invalidate()
			}
		}
	}

	@SuppressLint("DiscouragedApi")
	private fun getSystemUiDimensionOffset(name: String, fallback: Int = 0): Int = runCatching {
		val manager = context.packageManager
		val resources = manager.getResourcesForApplication("com.android.systemui")
		val resId = resources.getIdentifier(name, "dimen", "com.android.systemui")
		resources.getDimensionPixelOffset(resId)
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(fallback)
}
