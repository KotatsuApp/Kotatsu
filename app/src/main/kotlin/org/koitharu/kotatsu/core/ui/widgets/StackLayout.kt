package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.core.view.isGone

open class StackLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

	private val visibleChildren = ArrayList<View>()

	override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
		val w = r - l - paddingLeft - paddingRight
		val h = b - t - paddingTop - paddingBottom
		visibleChildren.clear()
		children.filterNotTo(visibleChildren) { it.isGone }
		if (w <= 0 || h <= 0 || visibleChildren.isEmpty()) {
			return
		}
		val xStep = w / (visibleChildren.size + 1)
		val yStep = h / (visibleChildren.size + 1)
		val maxW = w
		val maxH = h
		val total = visibleChildren.size
		for ((index, child) in visibleChildren.withIndex()) {
			var cx = paddingLeft + xStep * (total - index)
			var cy = paddingTop + yStep * (index + 1)
			val rx = child.measuredWidth.coerceAtMost(maxW) / 2
			val ry = child.measuredHeight.coerceAtMost(maxH) / 2
			if (cx < rx) {
				cx = rx
			}
			if (cy < ry) {
				cy = ry
			}
			if (cx + rx > width) {
				cx = width - rx
			}
			if (cy + ry > height) {
				cy = height - ry
			}
			child.layout(cx - rx, cy - ry, cx + rx, cy + ry)
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		measureChildren(widthMeasureSpec, heightMeasureSpec)
		if (isEmpty()) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			return
		}

		var h = 0
		var w = 0
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			if (child.isGone) {
				continue
			}
			val mw = child.measuredWidth
			val mh = child.measuredHeight
			if (h == 0 || w == 0) {
				h = mh
				w = mw
			} else {
				h += mh / 2
				w += mw / 2
			}
		}
		h += paddingTop + paddingBottom
		w += paddingLeft + paddingRight
		setMeasuredDimension(
			resolveSizeAndState(w, widthMeasureSpec, 0),
			resolveSizeAndState(h, heightMeasureSpec, 0),
		)
	}
}
