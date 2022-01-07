package org.koitharu.kotatsu.base.ui.list.decor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.utils.ext.getThemeDrawable
import kotlin.math.roundToInt

abstract class AbstractDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

	private val bounds = Rect()
	private val divider = context.getThemeDrawable(android.R.attr.listDivider)

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State,
	) {
		outRect.set(0, divider?.intrinsicHeight ?: 0, 0, 0)
	}

	// TODO implement for horizontal lists on demand
	override fun onDraw(canvas: Canvas, parent: RecyclerView, s: RecyclerView.State) {
		if (parent.layoutManager == null || divider == null) {
			return
		}
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

		var previous: RecyclerView.ViewHolder? = null
		for (child in parent.children) {
			val holder = parent.getChildViewHolder(child)
			if (previous != null && shouldDrawDivider(previous, holder)) {
				parent.getDecoratedBoundsWithMargins(child, bounds)
				val top: Int = bounds.top + child.translationY.roundToInt()
				val bottom: Int = top + divider.intrinsicHeight
				divider.setBounds(left, top, right, bottom)
				divider.draw(canvas)
			}
			previous = holder
		}
		canvas.restore()
	}

	protected abstract fun shouldDrawDivider(
		above: RecyclerView.ViewHolder,
		below: RecyclerView.ViewHolder,
	): Boolean
}