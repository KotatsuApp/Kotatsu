package org.koitharu.kotatsu.list.ui

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
import androidx.recyclerview.widget.RecyclerView.NO_ID
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import com.google.android.material.R as materialR

open class MangaSelectionDecoration(context: Context) : AbstractSelectionItemDecoration() {

	protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	protected val checkIcon = ContextCompat.getDrawable(context, materialR.drawable.ic_mtrl_checked_circle)
	protected val iconOffset = context.resources.getDimensionPixelOffset(R.dimen.card_indicator_offset)
	protected val iconSize = context.resources.getDimensionPixelOffset(R.dimen.card_indicator_size)
	protected val strokeColor = context.getThemeColor(materialR.attr.colorPrimary, Color.RED)
	protected val fillColor = ColorUtils.setAlphaComponent(
		ColorUtils.blendARGB(strokeColor, context.getThemeColor(materialR.attr.colorSurface), 0.8f),
		0x74,
	)
	protected val defaultRadius = context.resources.getDimension(R.dimen.list_selector_corner)

	init {
		hasBackground = false
		hasForeground = true
		isIncludeDecorAndMargins = false

		paint.strokeWidth = context.resources.getDimension(R.dimen.selection_stroke_width)
		checkIcon?.setTint(strokeColor)
	}

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return NO_ID
		val item = holder.getItem(MangaItemModel::class.java) ?: return NO_ID
		return item.id
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		val isCard = child is CardView
		val radius = (child as? CardView)?.radius ?: defaultRadius
		paint.color = fillColor
		paint.style = Paint.Style.FILL
		canvas.drawRoundRect(bounds, radius, radius, paint)
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, radius, radius, paint)
		if (isCard) {
			checkIcon?.run {
				setBounds(
					(bounds.left + iconOffset).toInt(),
					(bounds.top + iconOffset).toInt(),
					(bounds.left + iconOffset + iconSize).toInt(),
					(bounds.top + iconOffset + iconSize).toInt(),
				)
				draw(canvas)
			}
		}
	}
}
