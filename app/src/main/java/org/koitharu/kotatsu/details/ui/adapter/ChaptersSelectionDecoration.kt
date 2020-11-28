package org.koitharu.kotatsu.details.ui.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.collection.ArraySet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.resolveDp

class ChaptersSelectionDecoration(context: Context) : RecyclerView.ItemDecoration() {

	private val icon = ContextCompat.getDrawable(context, R.drawable.ic_check)
	private val padding = context.resources.resolveDp(16)
	private val bounds = Rect()
	private val selection = ArraySet<Long>()
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	init {
		paint.color = context.getThemeColor(com.google.android.material.R.attr.colorSurface)
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
		icon ?: return
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

	override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		icon ?: return
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

		for (child in parent.children) {
			val itemId = parent.getChildItemId(child)
			if (itemId in selection) {
				parent.getDecoratedBoundsWithMargins(child, bounds)
				bounds.offset(child.translationX.toInt(), child.translationY.toInt())
				val hh = (bounds.height() - icon.intrinsicHeight) / 2
				val top: Int = bounds.top + hh
				val bottom: Int = bounds.bottom - hh
				icon.setBounds(right - icon.intrinsicWidth - padding, top, right - padding, bottom)
				icon.draw(canvas)
			}
		}
		canvas.restore()
	}
}