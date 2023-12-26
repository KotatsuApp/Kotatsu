package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import androidx.core.view.ancestors
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.util.ext.resolveDp
import kotlin.math.roundToInt

class WebtoonImageView @JvmOverloads constructor(
	context: Context,
	attr: AttributeSet? = null,
) : SubsamplingScaleImageView(context, attr) {

	private val ct = PointF()

	private var scrollPos = 0
	private var debugPaint: Paint? = null

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		if (BuildConfig.DEBUG) {
			drawDebug(canvas)
		}
	}

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
		if (!isReady) {
			return 0
		}
		val totalHeight = (sHeight * width / sWidth.toFloat()).roundToInt()
		return (totalHeight - height).coerceAtLeast(0)
	}

	override fun recycle() {
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

	override fun onDownsamplingChanged() {
		super.onDownsamplingChanged()
		adjustScale()
	}

	override fun onReady() {
		super.onReady()
		adjustScale()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		if (oldh != h && oldw != 0 && oldh != 0 && isReady) {
			ancestors.firstNotNullOfOrNull { it as? WebtoonRecyclerView }?.updateChildrenScroll()
		} else {
			return
		}
	}

	private fun scrollToInternal(pos: Int) {
		minScale = width / sWidth.toFloat()
		maxScale = minScale
		scrollPos = pos
		ct.set(sWidth / 2f, (height / 2f + pos.toFloat()) / minScale)
		setScaleAndCenter(minScale, ct)
	}

	private fun adjustScale() {
		minScale = width / sWidth.toFloat()
		maxScale = minScale
		minimumScaleType = SCALE_TYPE_CUSTOM
	}

	private fun parentHeight(): Int {
		return ancestors.firstNotNullOfOrNull { it as? RecyclerView }?.height ?: 0
	}

	private fun drawDebug(canvas: Canvas) {
		val paint = debugPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = android.graphics.Color.RED
			strokeWidth = context.resources.resolveDp(2f)
			textAlign = android.graphics.Paint.Align.LEFT
			textSize = context.resources.resolveDp(14f)
			debugPaint = this
		}
		paint.style = Paint.Style.STROKE
		canvas.drawRect(1f, 1f, width.toFloat() - 1f, height.toFloat() - 1f, paint)
		paint.style = Paint.Style.FILL
		canvas.drawText("${getScroll()} / ${getScrollRange()}", 100f, 100f, paint)
	}
}
