package org.koitharu.kotatsu.details.ui.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import com.google.android.material.R as materialR

class ChaptersSelectionDecoration(context: Context) : AbstractSelectionItemDecoration() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val radius = context.resources.getDimension(materialR.dimen.abc_control_corner_material)
	private val checkIcon = ContextCompat.getDrawable(context, materialR.drawable.ic_mtrl_checked_circle)
	private val iconOffset = context.resources.getDimensionPixelOffset(R.dimen.chapter_check_offset)
	private val iconSize = context.resources.getDimensionPixelOffset(R.dimen.chapter_check_size)
	private val strokeColor = context.getThemeColor(materialR.attr.colorPrimary, Color.RED)
	private val fillColor = ColorUtils.setAlphaComponent(
		ColorUtils.blendARGB(strokeColor, context.getThemeColor(materialR.attr.colorSurface), 0.8f),
		0x74,
	)

	init {
		paint.color = ColorUtils.setAlphaComponent(
			context.getThemeColor(materialR.attr.colorPrimary, Color.DKGRAY),
			98,
		)
		paint.style = Paint.Style.FILL
		hasBackground = true
		hasForeground = true
		isIncludeDecorAndMargins = false

		paint.strokeWidth = context.resources.getDimension(R.dimen.selection_stroke_width)
		checkIcon?.setTint(strokeColor)
	}

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return RecyclerView.NO_ID
		val item = holder.getItem(ChapterListItem::class.java) ?: return RecyclerView.NO_ID
		return item.chapter.id
	}

	override fun onDrawBackground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		if (child is CardView) {
			return
		}
		canvas.drawRoundRect(bounds, radius, radius, paint)
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State
	) {
		if (child !is CardView) {
			return
		}
		val radius = child.radius
		paint.color = fillColor
		paint.style = Paint.Style.FILL
		canvas.drawRoundRect(bounds, radius, radius, paint)
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, radius, radius, paint)
		checkIcon?.run {
			setBounds(
				(bounds.right - iconSize - iconOffset).toInt(),
				(bounds.top + iconOffset).toInt(),
				(bounds.right - iconOffset).toInt(),
				(bounds.top + iconOffset + iconSize).toInt(),
			)
			draw(canvas)
		}
	}
}
