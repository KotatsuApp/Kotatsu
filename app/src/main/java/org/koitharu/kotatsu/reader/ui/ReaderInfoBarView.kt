package org.koitharu.kotatsu.reader.ui

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
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as materialR
import java.text.SimpleDateFormat
import java.util.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.measureDimension
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

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

	private var timeText = timeFormat.format(Date())
	private var text: String = ""

	private val innerHeight
		get() = height - paddingTop - paddingBottom - insetTop

	private val innerWidth
		get() = width - paddingLeft - paddingRight - insetLeft - insetRight

	init {
		paint.color = ColorUtils.setAlphaComponent(
			context.getThemeColor(materialR.attr.colorOnSurface, Color.BLACK),
			160,
		)
		paint.setShadowLayer(20f, 0f, 0f, context.getThemeColor(materialR.attr.colorOnSurfaceInverse, Color.WHITE))
		val insetStart = getSystemUiDimensionOffset("status_bar_padding_start")
		val insetEnd = getSystemUiDimensionOffset("status_bar_padding_end")
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
		canvas.drawText(text, (paddingLeft + insetLeft).toFloat(), paddingTop + insetTop + ty, paint)
		paint.textAlign = Paint.Align.RIGHT
		canvas.drawText(timeText, (width - paddingRight - insetRight).toFloat(), paddingTop + insetTop + ty, paint)
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		updateTextSize()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		context.registerReceiver(timeReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		context.unregisterReceiver(timeReceiver)
	}

	fun update(state: ReaderUiState?) {
		text = if (state != null) {
			val percent = state.computePercent()
			context.getString(
				R.string.reader_info_pattern,
				state.chapterNumber,
				state.chaptersTotal,
				state.currentPage + 1,
				state.totalPages,
			) + if (percent in 0f..1f) {
				"     " + context.getString(R.string.percent_string_pattern, (percent * 100).format())
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

	private inner class TimeReceiver : BroadcastReceiver() {

		override fun onReceive(context: Context?, intent: Intent?) {
			timeText = timeFormat.format(Date())
			invalidate()
		}
	}

	private fun getSystemUiDimensionOffset(name: String): Int = runCatching {
		val manager = context.packageManager
		val resources = manager.getResourcesForApplication("com.android.systemui")
		val resId = resources.getIdentifier(name, "dimen", "com.android.systemui")
		resources.getDimensionPixelOffset(resId)
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(0)
}
