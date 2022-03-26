package org.koitharu.kotatsu.details.ui.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R

class ChaptersSelectionDecoration(context: Context) : RecyclerView.ItemDecoration() {

	private val bounds = Rect()
	private val selection = HashSet<Long>()
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	init {
		paint.color = ContextCompat.getColor(context, R.color.selector_foreground)
		paint.style = Paint.Style.FILL
	}

	val checkedItemsCount: Int
		get() = selection.size

	val checkedItemsIds: Set<Long>
		get() = selection

	fun toggleItemChecked(id: Long) {
		if (!selection.remove(id)) {
			selection.add(id)
		}
	}

	fun setItemIsChecked(id: Long, isChecked: Boolean) {
		if (isChecked) {
			selection.add(id)
		} else {
			selection.remove(id)
		}
	}

	fun checkAll(ids: Collection<Long>) {
		selection.addAll(ids)
	}

	fun clearSelection() {
		selection.clear()
	}

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		canvas.save()
		if (parent.clipToPadding) {
			canvas.clipRect(
				parent.paddingLeft, parent.paddingTop, parent.width - parent.paddingRight,
				parent.height - parent.paddingBottom
			)
		}

		for (child in parent.children) {
			val itemId = parent.getChildItemId(child)
			if (itemId in selection) {
				parent.getDecoratedBoundsWithMargins(child, bounds)
				bounds.offset(child.translationX.toInt(), child.translationY.toInt())
				canvas.drawRect(bounds, paint)
			}
		}
		canvas.restore()
	}
}