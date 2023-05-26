package org.koitharu.kotatsu.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.ui.model.BookmarksGroup
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.errorStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.parsers.model.Manga
import kotlin.jvm.internal.Intrinsics

class BookmarksGroupAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	selectionController: SectionedSelectionController<Manga>,
	listener: ListStateHolderListener,
	bookmarkClickListener: OnListItemClickListener<Bookmark>,
	groupClickListener: OnListItemClickListener<BookmarksGroup>,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		val pool = RecyclerView.RecycledViewPool()
		delegatesManager
			.addDelegate(
				bookmarksGroupAD(
					coil = coil,
					lifecycleOwner = lifecycleOwner,
					sharedPool = pool,
					selectionController = selectionController,
					bookmarkClickListener = bookmarkClickListener,
					groupClickListener = groupClickListener,
				),
			)
			.addDelegate(loadingStateAD())
			.addDelegate(loadingFooterAD())
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, listener))
			.addDelegate(errorStateListAD(listener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is BookmarksGroup && newItem is BookmarksGroup -> {
					oldItem.manga.id == newItem.manga.id
				}

				oldItem is LoadingFooter && newItem is LoadingFooter -> {
					oldItem.key == newItem.key
				}

				else -> oldItem.javaClass == newItem.javaClass
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}

		override fun getChangePayload(oldItem: ListModel, newItem: ListModel): Any? {
			return when {
				oldItem is BookmarksGroup && newItem is BookmarksGroup -> Unit
				else -> super.getChangePayload(oldItem, newItem)
			}
		}
	}
}
