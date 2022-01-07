package org.koitharu.kotatsu.settings.sources.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.base.ui.list.decor.AbstractDividerItemDecoration

class SourceConfigItemDecoration(context: Context) : AbstractDividerItemDecoration(context) {

	override fun shouldDrawDivider(
		above: RecyclerView.ViewHolder,
		below: RecyclerView.ViewHolder,
	): Boolean {
		return above.itemViewType != 0 && below.itemViewType != 0
	}
}