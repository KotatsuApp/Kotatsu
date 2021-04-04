package org.koitharu.kotatsu.reader.ui.pager.wetoon

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.utils.ext.toIntUp

class WebtoonImageView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) :
	SubsamplingScaleImageView(context, attr) {

	private val ct = PointF()
	private val displayHeight = resources.displayMetrics.heightPixels

	private var scrollPos = 0
	private var scrollRange = SCROLL_UNKNOWN

	fun scrollBy(delta: Int) {
		val maxScroll = getScrollRange()
		if (maxScroll == 0) {
			return
		}
		val newScroll = scrollPos + delta
		scrollToInternal(newScroll.coerceIn(0, maxScroll))
	}

	fun scrollTo(y: Int) {
		val maxScroll = getScrollRange()
		if (maxScroll == 0) {
			return
		}
		scrollToInternal(y.coerceIn(0, maxScroll))
	}

	fun getScroll() = scrollPos

	fun getScrollRange(): Int {
		if (scrollRange == SCROLL_UNKNOWN) {
			computeScrollRange()
		}
		return scrollRange.coerceAtLeast(0)
	}

	override fun recycle() {
		scrollRange = SCROLL_UNKNOWN
		scrollPos = 0
		super.recycle()
	}

	override fun getSuggestedMinimumHeight(): Int {
		var desiredHeight = super.getSuggestedMinimumHeight()
		if (sHeight == 0 && desiredHeight < displayHeight) {
			desiredHeight = displayHeight
		}
		return desiredHeight
	}

	private fun scrollToInternal(pos: Int) {
		scrollPos = pos
		ct.set(sWidth / 2f, (height / 2f + pos.toFloat()) / minScale)
		setScaleAndCenter(minScale, ct)
	}

	private fun computeScrollRange() {
		if (!isReady) {
			return
		}
		val totalHeight = (sHeight * minScale).toIntUp()
		scrollRange = (totalHeight - height).coerceAtLeast(0)
	}

	private companion object {

		const val SCROLL_UNKNOWN = -1
	}
}