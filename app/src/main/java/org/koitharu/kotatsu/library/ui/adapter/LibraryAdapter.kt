package org.koitharu.kotatsu.library.ui.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlin.jvm.internal.Intrinsics
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.errorStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class LibraryAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: LibraryListEventListener,
	sizeResolver: ItemSizeResolver,
	selectionController: SectionedSelectionController<LibrarySectionModel>,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()), FastScroller.SectionIndexer {

	init {
		val pool = RecyclerView.RecycledViewPool()
		delegatesManager
			.addDelegate(
				libraryGroupAD(
					sharedPool = pool,
					lifecycleOwner = lifecycleOwner,
					coil = coil,
					sizeResolver = sizeResolver,
					selectionController = selectionController,
					listener = listener,
				),
			)
			.addDelegate(loadingStateAD())
			.addDelegate(loadingFooterAD())
			.addDelegate(emptyStateListAD(listener))
			.addDelegate(errorStateListAD(listener))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence {
		val item = items.getOrNull(position) as? LibrarySectionModel
		return item?.getTitle(context.resources) ?: ""
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is LibrarySectionModel && newItem is LibrarySectionModel -> {
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
				oldItem is LibrarySectionModel && newItem is LibrarySectionModel -> Unit
				else -> super.getChangePayload(oldItem, newItem)
			}
		}
	}
}
