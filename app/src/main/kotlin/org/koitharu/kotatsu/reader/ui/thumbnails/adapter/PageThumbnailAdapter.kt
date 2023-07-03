package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.adapter.listHeaderAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail

class PageThumbnailAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<PageThumbnail>,
) : AsyncListDifferDelegationAdapter<ListModel>(ListModelDiffCallback), FastScroller.SectionIndexer {

	init {
		delegatesManager.addDelegate(ITEM_TYPE_THUMBNAIL, pageThumbnailAD(coil, lifecycleOwner, clickListener))
			.addDelegate(ITEM_TYPE_HEADER, listHeaderAD(null))
			.addDelegate(ITEM_LOADING, loadingFooterAD())
	}

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

	companion object {

		const val ITEM_TYPE_THUMBNAIL = 0
		const val ITEM_TYPE_HEADER = 1
		const val ITEM_LOADING = 2
	}
}
