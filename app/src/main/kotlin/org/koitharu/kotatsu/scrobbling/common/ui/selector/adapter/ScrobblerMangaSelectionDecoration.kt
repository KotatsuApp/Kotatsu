package org.koitharu.kotatsu.scrobbling.common.ui.selector.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga

class ScrobblerMangaSelectionDecoration(context: Context) : MangaSelectionDecoration(context) {

	var checkedItemId: Long
		get() = if (selection.size == 1) {
			selection.first()
		} else {
			NO_ID
		}
		set(value) {
			clearSelection()
			if (value != NO_ID) {
				selection.add(value)
			}
		}

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return NO_ID
		val item = holder.getItem(ScrobblerManga::class.java) ?: return NO_ID
		return item.id
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, defaultRadius, defaultRadius, paint)
	}
}
