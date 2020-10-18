package org.koitharu.kotatsu.ui.base.list.decor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.utils.ext.getThemeDrawable
import kotlin.math.roundToInt

class ItemTypeDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {

	private val divider = context.getThemeDrawable(android.R.attr.listDivider)
	private val bounds = Rect()

	override fun getItemOffsets(
		outRect: Rect, view: View,
		parent: RecyclerView, state: RecyclerView.State
	) {
		outRect.set(0, divider?.intrinsicHeight ?: 0, 0, 0)
	}

	override fun onDraw(canvas: Canvas, parent: RecyclerView, s: RecyclerView.State) {
		if (parent.layoutManager == null || divider == null) {
			return
		}
		val adapter = parent.adapter ?: return
		canvas.save()
		val left: Int
		val right: Int
		if (parent.clipToPadding) {
			left = parent.paddingLeft
			right = parent.width - parent.paddingRight
			canvas.clipRect(
				left, parent.paddingTop, right,
				parent.height - parent.paddingBottom
			)
		} else {
			left = 0
			right = parent.width
		}

		var lastItemType = -1
		for (child in parent.children) {
			val itemType = adapter.getItemViewType(parent.getChildAdapterPosition(child))
			if (lastItemType != -1 && itemType != lastItemType) {
				parent.getDecoratedBoundsWithMargins(child, bounds)
				val top: Int = bounds.top + child.translationY.roundToInt()
				val bottom: Int = top + divider.intrinsicHeight
				divider.setBounds(left, top, right, bottom)
				divider.draw(canvas)
			}
			lastItemType = itemType
		}
		canvas.restore()
	}
}