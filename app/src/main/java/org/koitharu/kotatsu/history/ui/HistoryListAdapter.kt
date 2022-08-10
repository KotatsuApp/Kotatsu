package org.koitharu.kotatsu.history.ui

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.base.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener

class HistoryListAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener
) : MangaListAdapter(coil, lifecycleOwner, listener), FastScroller.SectionIndexer {

	override fun getSectionText(context: Context, position: Int): CharSequence {
		val list = items
		for (i in (0..position).reversed()) {
			val item = list[i]
			if (item is DateTimeAgo) {
				return item.format(context.resources)
			}
		}
		return ""
	}
}