package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.list.ui.adapter.listHeaderAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail

class PageThumbnailAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<PageThumbnail>,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()), FastScroller.SectionIndexer {

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

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is PageThumbnail && newItem is PageThumbnail -> {
					oldItem.page == newItem.page
				}

				oldItem is ListHeader && newItem is ListHeader -> {
					oldItem.textRes == newItem.textRes &&
						oldItem.text == newItem.text &&
						oldItem.dateTimeAgo == newItem.dateTimeAgo
				}

				oldItem is LoadingFooter && newItem is LoadingFooter -> {
					oldItem.key == newItem.key
				}

				else -> oldItem.javaClass == newItem.javaClass
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return oldItem == newItem
		}
	}

	companion object {

		const val ITEM_TYPE_THUMBNAIL = 0
		const val ITEM_TYPE_HEADER = 1
		const val ITEM_LOADING = 2
	}
}
