package org.koitharu.kotatsu.core.ui.list.decor

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.collection.LongSet
import androidx.collection.MutableLongSet
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID

abstract class AbstractSelectionItemDecoration : RecyclerView.ItemDecoration() {

	private val bounds = Rect()
	private val boundsF = RectF()
	protected val selection = MutableLongSet()

	protected var hasBackground: Boolean = true
	protected var hasForeground: Boolean = false
	protected var isIncludeDecorAndMargins: Boolean = true

	val checkedItemsCount: Int
		get() = selection.size

	val checkedItemsIds: LongSet
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
		for (id in ids) {
			selection.add(id)
		}
	}

	fun clearSelection() {
		selection.clear()
	}

	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		if (hasBackground) {
			doDraw(canvas, parent, state, false)
		} else {
			super.onDraw(canvas, parent, state)
		}
	}

	override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		if (hasForeground) {
			doDraw(canvas, parent, state, true)
		} else {
			super.onDrawOver(canvas, parent, state)
		}
	}

	private fun doDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State, isOver: Boolean) {
		val checkpoint = canvas.save()
		if (parent.clipToPadding) {
			canvas.clipRect(
				parent.paddingLeft, parent.paddingTop, parent.width - parent.paddingRight,
				parent.height - parent.paddingBottom,
			)
		}

		for (child in parent.children) {
			val itemId = getItemId(parent, child)
			if (itemId != NO_ID && itemId in selection) {
				if (isIncludeDecorAndMargins) {
					parent.getDecoratedBoundsWithMargins(child, bounds)
				} else {
					bounds.set(child.left, child.top, child.right, child.bottom)
				}
				boundsF.set(bounds)
				boundsF.offset(child.translationX, child.translationY)
				if (isOver) {
					onDrawForeground(canvas, parent, child, boundsF, state)
				} else {
					onDrawBackground(canvas, parent, child, boundsF, state)
				}
			}
		}
		canvas.restoreToCount(checkpoint)
	}

	abstract fun getItemId(parent: RecyclerView, child: View): Long

	protected open fun onDrawBackground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) = Unit

	protected open fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) = Unit
}
