package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.utils.ext.toIntUp

class WebtoonImageView : SubsamplingScaleImageView {

	constructor(context: Context?) : super(context)
	constructor(context: Context?, attr: AttributeSet?) : super(context, attr)

	private val pan = RectF()
	private val ct = PointF()

	fun dispatchVerticalScroll(dy: Int): Int {
		if (!isReady) {
			return 0
		}
		getPanRemaining(pan)
		// pan.offset(0f, -nonConsumedScroll.toFloat())
		ct.set(width / 2f, height / 2f)
		viewToSourceCoord(ct.x, ct.y, ct) ?: return 0
		val s = scale
		return when {
			dy > 0 -> {
				val delta = minOf(pan.bottom.toIntUp(), dy)
				ct.offset(0f, delta.toFloat() / s)
				setScaleAndCenter(s, ct)
				delta
			}
			dy < 0 -> {
				val delta = minOf(pan.top.toInt(), -dy)
				ct.offset(0f, -delta.toFloat() / s)
				setScaleAndCenter(s, ct)
				-delta
			}
			else -> 0
		}
	}
}