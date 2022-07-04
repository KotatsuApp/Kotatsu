package org.koitharu.kotatsu.library.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.library.ui.model.LibraryGroupModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.*
import org.koitharu.kotatsu.list.ui.model.ListModel
import kotlin.jvm.internal.Intrinsics

class LibraryAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: MangaListListener,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
	itemClickListener: OnListItemClickListener<LibraryGroupModel>,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		val pool = RecyclerView.RecycledViewPool()
		delegatesManager
			.addDelegate(
				libraryGroupAD(
					sharedPool = pool,
					lifecycleOwner = lifecycleOwner,
					coil = coil,
					sizeResolver = sizeResolver,
					selectionDecoration = selectionDecoration,
					listener = listener,
					itemClickListener = itemClickListener,
				)
			)
			.addDelegate(loadingStateAD())
			.addDelegate(loadingFooterAD())
			.addDelegate(emptyStateListAD(listener))
			.addDelegate(errorStateListAD(listener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is LibraryGroupModel && newItem is LibraryGroupModel -> {
					oldItem.key == newItem.key
				}
				else -> oldItem.javaClass == newItem.javaClass
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}
}