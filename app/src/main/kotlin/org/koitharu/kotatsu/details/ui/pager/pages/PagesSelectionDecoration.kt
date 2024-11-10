package org.koitharu.kotatsu.details.ui.pager.pages

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.util.ext.getItem
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration

class PagesSelectionDecoration(context: Context) : MangaSelectionDecoration(context) {

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return RecyclerView.NO_ID
		val item = holder.getItem(PageThumbnail::class.java) ?: return RecyclerView.NO_ID
		return item.page.id
	}
}
