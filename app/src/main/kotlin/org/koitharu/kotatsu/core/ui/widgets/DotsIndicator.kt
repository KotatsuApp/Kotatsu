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

class DotsIndicator @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private var indicatorSize = context.resources.resolveDp(12f)
	private var dotSpacing = 0f
	private var positionOffset: Float = 0f
	var max: Int = 6
		set(value) {
			if (field != value) {
				field = value
				requestLayout()
				invalidate()
			}
		}
	var position: Int = 2
		set(value) {
			if (field != value) {
				field = value
				invalidate()
			}
		}

	init {
		paint.strokeWidth = context.resources.resolveDp(1.5f)
		context.withStyledAttributes(attrs, R.styleable.DotsIndicator, defStyleAttr) {
			paint.color = getColor(
				R.styleable.DotsIndicator_dotColor,
				context.getThemeColor(com.google.android.material.R.attr.colorPrimary, Color.DKGRAY),
			)
			indicatorSize = getDimension(R.styleable.DotsIndicator_dotSize, indicatorSize)
			dotSpacing = getDimension(R.styleable.DotsIndicator_dotSpacing, dotSpacing)
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val dotSize = getDotSize()
		val y = paddingTop + (height - paddingTop - paddingBottom) / 2f
		var x = paddingLeft + dotSize / 2f
		val radius = dotSize / 2f - paint.strokeWidth
		val spacing = (width - paddingLeft - paddingRight) / max.toFloat() - dotSize
		x += spacing / 2f
		paint.style = Paint.Style.STROKE
		for (i in 0 until max) {
			canvas.drawCircle(x, y, radius, paint)
			if (i == position) {
				paint.style = Paint.Style.FILL
				paint.alpha = (255 * (1f - positionOffset)).toInt()
				canvas.drawCircle(x, y, radius, paint)
				paint.alpha = 255
				paint.style = Paint.Style.STROKE
			}
			if (i == position + 1 && positionOffset > 0f) {
				paint.style = Paint.Style.FILL
				paint.alpha = (255 * positionOffset).toInt()
				canvas.drawCircle(x, y, radius, paint)
				paint.alpha = 255
				paint.style = Paint.Style.STROKE
			}
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

		override fun onPageSelected(position: Int) {
			super.onPageSelected(position)
			this@DotsIndicator.position = position
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
