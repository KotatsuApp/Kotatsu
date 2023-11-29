package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import androidx.core.view.ancestors
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.parsers.util.toIntUp

private const val SCROLL_UNKNOWN = -1

class WebtoonImageView @JvmOverloads constructor(
	context: Context,
	attr: AttributeSet? = null,
) : SubsamplingScaleImageView(context, attr) {

	private val ct = PointF()

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
			resetScaleAndCenter()
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
		if (sHeight == 0) {
			val parentHeight = parentHeight()
			if (desiredHeight < parentHeight) {
				desiredHeight = parentHeight
			}
		}
		return desiredHeight
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
		val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
		val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
		val resizeWidth = widthSpecMode != MeasureSpec.EXACTLY
		val resizeHeight = heightSpecMode != MeasureSpec.EXACTLY
		var width = parentWidth
		var height = parentHeight
		if (sWidth > 0 && sHeight > 0) {
			if (resizeWidth && resizeHeight) {
				width = sWidth
				height = sHeight
			} else if (resizeHeight) {
				height = (sHeight.toDouble() / sWidth.toDouble() * width).toInt()
			} else if (resizeWidth) {
				width = (sWidth.toDouble() / sHeight.toDouble() * height).toInt()
			}
		}
		width = width.coerceAtLeast(suggestedMinimumWidth)
		height = height.coerceAtLeast(suggestedMinimumHeight).coerceAtMost(parentHeight())
		setMeasuredDimension(width, height)
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		if (oldh == h || oldw == 0 || oldh == 0 || scrollRange == SCROLL_UNKNOWN) return

		computeScrollRange()
		val container = ancestors.firstNotNullOfOrNull { it as? WebtoonFrameLayout } ?: return
		val parentHeight = parentHeight()
		if (scrollPos != 0 && container.bottom < parentHeight) {
			scrollTo(scrollRange)
		}
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

	private fun parentHeight(): Int {
		return ancestors.firstNotNullOfOrNull { it as? RecyclerView }?.height ?: 0
	}
}
