package org.koitharu.kotatsu.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R

class NestedRecyclerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

	private var maxHeight: Int = 0

	init {
		context.withStyledAttributes(attrs, R.styleable.NestedRecyclerView) {
			maxHeight = getDimensionPixelSize(R.styleable.NestedRecyclerView_maxHeight, maxHeight)
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(e: MotionEvent?): Boolean {
		if (e?.actionMasked == MotionEvent.ACTION_UP) {
			requestDisallowInterceptTouchEvent(false)
		} else {
			requestDisallowInterceptTouchEvent(true)
		}
		return super.onTouchEvent(e)
	}

	override fun onMeasure(widthSpec: Int, heightSpec: Int) {
		super.onMeasure(
			widthSpec,
			if (maxHeight == 0) {
				heightSpec
			} else {
				MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
			},
		)
	}
}
