package org.koitharu.kotatsu.history.ui

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListHeader

class HistoryListAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener
) : MangaListAdapter(coil, lifecycleOwner, listener), FastScroller.SectionIndexer {

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		val list = items
		for (i in (0..position).reversed()) {
			val item = list.getOrNull(i) ?: continue
			if (item is ListHeader) {
				return item.getText(context)
			}
		}
		return null
	}
}
