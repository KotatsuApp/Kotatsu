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
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.measureDimension
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import java.text.SimpleDateFormat
import java.util.Date
import com.google.android.material.R as materialR

class ReaderInfoBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textBounds = Rect()
	private val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
	private val timeReceiver = TimeReceiver()
	private var insetLeft: Int = 0
	private var insetRight: Int = 0
	private var insetTop: Int = 0
	private var cutoutInsetLeft = 0
	private var cutoutInsetRight = 0
	private val colorText = ColorUtils.setAlphaComponent(
		context.getThemeColor(materialR.attr.colorOnSurface, Color.BLACK),
		200,
	)
	private val colorOutline = ColorUtils.setAlphaComponent(
		context.getThemeColor(materialR.attr.colorSurface, Color.WHITE),
		200,
	)

	private var timeText = timeFormat.format(Date())
	private var text: String = ""

	private val innerHeight
		get() = height - paddingTop - paddingBottom - insetTop

	private val innerWidth
		get() = width - paddingLeft - paddingRight - insetLeft - insetRight

	init {
		paint.strokeWidth = context.resources.resolveDp(2f)
		val insetCorner = getSystemUiDimensionOffset("rounded_corner_content_padding")
		val fallbackInset = resources.getDimensionPixelOffset(R.dimen.reader_bar_inset_fallback)
		val insetStart = getSystemUiDimensionOffset("status_bar_padding_start", fallbackInset) + insetCorner
		val insetEnd = getSystemUiDimensionOffset("status_bar_padding_end", fallbackInset) + insetCorner
		val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
		insetLeft = if (isRtl) insetEnd else insetStart
		insetRight = if (isRtl) insetStart else insetEnd
		insetTop = minOf(insetLeft, insetRight)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight + insetLeft + insetRight
		val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom + insetTop
		setMeasuredDimension(
			measureDimension(desiredWidth, widthMeasureSpec),
			measureDimension(desiredHeight, heightMeasureSpec),
		)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val ty = innerHeight / 2f + textBounds.height() / 2f - textBounds.bottom
		paint.textAlign = Paint.Align.LEFT
		canvas.drawTextOutline(
			text,
			(paddingLeft + insetLeft + cutoutInsetLeft).toFloat(),
			paddingTop + insetTop + ty,
		)
		paint.textAlign = Paint.Align.RIGHT
		canvas.drawTextOutline(
			timeText,
			(width - paddingRight - insetRight - cutoutInsetRight).toFloat(),
			paddingTop + insetTop + ty,
		)
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		updateCutoutInsets(ViewCompat.getRootWindowInsets(this))
		updateTextSize()
	}

	override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
		updateCutoutInsets(WindowInsetsCompat.toWindowInsetsCompat(insets))
		return super.onApplyWindowInsets(insets)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		ContextCompat.registerReceiver(
			context,
			timeReceiver,
			IntentFilter(Intent.ACTION_TIME_TICK),
			ContextCompat.RECEIVER_EXPORTED,
		)
		updateCutoutInsets(ViewCompat.getRootWindowInsets(this))
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		context.unregisterReceiver(timeReceiver)
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
		updateTextSize()
		invalidate()
	}

	private fun updateTextSize() {
		val str = text + timeText
		val testTextSize = 48f
		paint.textSize = testTextSize
		paint.getTextBounds(str, 0, str.length, textBounds)
		paint.textSize = testTextSize * innerHeight / textBounds.height()
		paint.getTextBounds(str, 0, str.length, textBounds)
	}

	private fun Canvas.drawTextOutline(text: String, x: Float, y: Float) {
		paint.color = colorOutline
		paint.style = Paint.Style.STROKE
		drawText(text, x, y, paint)
		paint.color = colorText
		paint.style = Paint.Style.FILL
		drawText(text, x, y, paint)
	}

	private fun updateCutoutInsets(insetsCompat: WindowInsetsCompat?) {
		val cutouts = (insetsCompat ?: return).displayCutout?.boundingRects.orEmpty()
		cutoutInsetLeft = 0
		cutoutInsetRight = 0
		for (rect in cutouts) {
			if (rect.left <= paddingLeft) {
				cutoutInsetLeft += rect.width()
			}
			if (rect.right >= width - paddingRight) {
				cutoutInsetRight += rect.width()
			}
		}
	}

	private inner class TimeReceiver : BroadcastReceiver() {

		override fun onReceive(context: Context?, intent: Intent?) {
			timeText = timeFormat.format(Date())
			invalidate()
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
