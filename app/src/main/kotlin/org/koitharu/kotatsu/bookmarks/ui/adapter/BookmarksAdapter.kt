package org.koitharu.kotatsu.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener

class BookmarksAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Bookmark>,
) : AsyncListDifferDelegationAdapter<Bookmark>(
	DiffCallback(),
	bookmarkListAD(coil, lifecycleOwner, clickListener),
) {

	private class DiffCallback : DiffUtil.ItemCallback<Bookmark>() {

		override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
			return oldItem.manga.id == newItem.manga.id &&
				oldItem.chapterId == newItem.chapterId &&
				oldItem.page == newItem.page
		}

		override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
			return oldItem.imageUrl == newItem.imageUrl
		}

	}
}
