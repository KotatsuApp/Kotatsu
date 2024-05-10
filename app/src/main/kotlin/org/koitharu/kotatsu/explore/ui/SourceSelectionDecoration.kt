package org.koitharu.kotatsu.explore.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.explore.ui.model.MangaSourceItem
import com.google.android.material.R as materialR

class SourceSelectionDecoration(context: Context) : AbstractSelectionItemDecoration() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
	}

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return NO_ID
		val item = holder.getItem(MangaSourceItem::class.java) ?: return NO_ID
		return item.id
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		paint.color = fillColor
		paint.style = Paint.Style.FILL
		canvas.drawRoundRect(bounds, defaultRadius, defaultRadius, paint)
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, defaultRadius, defaultRadius, paint)
	}
}
