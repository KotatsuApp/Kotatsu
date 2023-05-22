package org.koitharu.kotatsu.download.ui.list

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
import com.google.android.material.R as materialR

class DownloadsSelectionDecoration(context: Context) : AbstractSelectionItemDecoration() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val checkIcon = ContextCompat.getDrawable(context, materialR.drawable.ic_mtrl_checked_circle)
	private val iconOffset = context.resources.getDimensionPixelOffset(R.dimen.card_indicator_offset)
	private val iconSize = context.resources.getDimensionPixelOffset(R.dimen.card_indicator_size)
	private val strokeColor = context.getThemeColor(materialR.attr.colorPrimary, Color.RED)
	private val fillColor = ColorUtils.setAlphaComponent(
		ColorUtils.blendARGB(strokeColor, context.getThemeColor(materialR.attr.colorSurface), 0.8f),
		0x74,
	)
	private val defaultRadius = context.resources.getDimension(R.dimen.list_selector_corner)

	init {
		hasBackground = false
		hasForeground = true
		isIncludeDecorAndMargins = false

		paint.strokeWidth = context.resources.getDimension(R.dimen.selection_stroke_width)
		checkIcon?.setTint(strokeColor)
	}

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return NO_ID
		val item = holder.getItem(DownloadItemModel::class.java) ?: return NO_ID
		return item.id.mostSignificantBits
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
					(bounds.right - iconSize - iconOffset).toInt(),
					(bounds.top + iconOffset).toInt(),
					(bounds.right - iconOffset).toInt(),
					(bounds.top + iconOffset + iconSize).toInt(),
				)
				draw(canvas)
			}
		}
	}
}
