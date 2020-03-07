package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page_webtoon.view.*
import org.koitharu.kotatsu.R

class WebtoonFrameLayout @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private val pan = RectF()

	private val target by lazy {
		findViewById<SubsamplingScaleImageView>(R.id.ssiv)
	}

	fun dispatchVerticalScroll(dy: Int): Int {
		if (!target.isImageLoaded) {
			return dy //consume all scrolling
		}
		target.getPanRemaining(pan)
		val c = target.center ?: return 0
		val s = target.scale
		return when {
			dy > 0 -> {
				val delta = minOf(pan.bottom.toInt(), dy)
				c.offset(0f, delta.toFloat() / s)
				target.setScaleAndCenter(s, c)
				delta
			}
			dy < 0 -> {
				val delta = minOf(pan.top.toInt(), -dy)
				c.offset(0f, -delta.toFloat() / s)
				target.setScaleAndCenter(s, c)
				-delta
			}
			else -> 0
		}
	}
}