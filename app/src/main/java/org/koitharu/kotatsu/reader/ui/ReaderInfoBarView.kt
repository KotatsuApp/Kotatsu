package org.koitharu.kotatsu.reader.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as materialR
import java.util.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.resolveDp

class ReaderInfoBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textBounds = Rect()
	private val inset = context.resources.resolveDp(2f)
	private val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
	private val timeReceiver = TimeReceiver()

	private var timeText = timeFormat.format(Date())
	private var text: String = ""

	private val innerHeight
		get() = height - inset - inset - paddingTop - paddingBottom

	private val innerWidth
		get() = width - inset - inset - paddingLeft - paddingRight

	init {
		paint.color = ColorUtils.setAlphaComponent(
			context.getThemeColor(materialR.attr.colorOnSurface, Color.BLACK),
			160,
		)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val ty = innerHeight / 2f + textBounds.height() / 2f - textBounds.bottom
		paint.textAlign = Paint.Align.LEFT
		canvas.drawText(text, paddingLeft + inset, paddingTop + inset + ty, paint)
		paint.textAlign = Paint.Align.RIGHT
		canvas.drawText(timeText, width - paddingRight - inset, paddingTop + inset + ty, paint)
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
}
