package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.viewpager2.widget.ViewPager2
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.measureDimension
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.toIntUp
import com.google.android.material.R as materialR

class DotsIndicator @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = R.attr.dotIndicatorStyle,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private var indicatorSize = context.resources.resolveDp(12f)
	private var dotSpacing = 0f
	private var smallDotScale = 0.33f
	private var smallDotAlpha = 0.6f
	private var positionOffset: Float = 0f
	private var position: Int = 0
	private val inset = context.resources.resolveDp(1f)

	var max: Int = 6
		set(value) {
			if (field != value) {
				field = value
				requestLayout()
				invalidate()
			}
		}
	var progress: Int
		get() = position
		set(value) {
			if (position != value) {
				position = value
				invalidate()
			}
		}

	init {
		paint.style = Paint.Style.FILL
		context.withStyledAttributes(attrs, R.styleable.DotsIndicator, defStyleAttr) {
			paint.color = getColor(
				R.styleable.DotsIndicator_dotColor,
				context.getThemeColor(materialR.attr.colorOnBackground, Color.DKGRAY),
			)
			indicatorSize = getDimension(R.styleable.DotsIndicator_dotSize, indicatorSize)
			dotSpacing = getDimension(R.styleable.DotsIndicator_dotSpacing, dotSpacing)
			smallDotScale = getFloat(R.styleable.DotsIndicator_dotScale, smallDotScale).coerceIn(0f, 1f)
			smallDotAlpha = getFloat(R.styleable.DotsIndicator_dotAlpha, smallDotAlpha).coerceIn(0f, 1f)
			max = getInt(R.styleable.DotsIndicator_android_max, max)
			position = getInt(R.styleable.DotsIndicator_android_progress, position)
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val dotSize = getDotSize()
		val y = paddingTop + (height - paddingTop - paddingBottom) / 2f
		var x = paddingLeft + dotSize / 2f
		val radius = dotSize / 2f - inset
		val spacing = (width - paddingLeft - paddingRight) / max.toFloat() - dotSize
		x += spacing / 2f
		for (i in 0 until max) {
			val scale = when (i) {
				position -> (1f - smallDotScale) * (1f - positionOffset) + smallDotScale
				position + 1 -> (1f - smallDotScale) * positionOffset + smallDotScale
				else -> smallDotScale
			}
			paint.alpha = (255 * when (i) {
				position -> (1f - smallDotAlpha) * (1f - positionOffset) + smallDotAlpha
				position + 1 -> (1f - smallDotAlpha) * positionOffset + smallDotAlpha
				else -> smallDotAlpha
			}).toInt()
			canvas.drawCircle(x, y, radius * scale, paint)
			x += spacing + dotSize
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val dotSize = getDotSize()
		val desiredHeight = (dotSize + paddingTop + paddingBottom).toIntUp()
		val desiredWidth = ((dotSize + dotSpacing) * max).toIntUp() + paddingLeft + paddingRight
		setMeasuredDimension(
			measureDimension(desiredWidth, widthMeasureSpec),
			measureDimension(desiredHeight, heightMeasureSpec),
		)
	}

	fun bindToViewPager(pager: ViewPager2) {
		pager.registerOnPageChangeCallback(ViewPagerCallback())
		pager.adapter?.let {
			it.registerAdapterDataObserver(AdapterObserver(it))
		}
	}

	private fun getDotSize() = if (indicatorSize <= 0) {
		(height - paddingTop - paddingBottom).toFloat()
	} else {
		indicatorSize
	}

	private inner class ViewPagerCallback : ViewPager2.OnPageChangeCallback() {

		override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
			super.onPageScrolled(position, positionOffset, positionOffsetPixels)
			this@DotsIndicator.position = position
			this@DotsIndicator.positionOffset = positionOffset
			invalidate()
		}
	}

	private inner class AdapterObserver(
		private val adapter: RecyclerView.Adapter<*>,
	) : AdapterDataObserver() {

		override fun onChanged() {
			super.onChanged()
			max = adapter.itemCount
		}

		override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
			super.onItemRangeInserted(positionStart, itemCount)
			max = adapter.itemCount
		}

		override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
			super.onItemRangeRemoved(positionStart, itemCount)
			max = adapter.itemCount
		}
	}
}
