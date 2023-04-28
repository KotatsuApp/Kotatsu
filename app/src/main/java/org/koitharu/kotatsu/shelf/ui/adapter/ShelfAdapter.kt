package org.koitharu.kotatsu.shelf.ui.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.NestedScrollStateHandle
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.adapter.emptyHintAD
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.errorStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.shelf.ui.model.ShelfSectionModel
import kotlin.jvm.internal.Intrinsics

class ShelfAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: ShelfListEventListener,
	sizeResolver: ItemSizeResolver,
	selectionController: SectionedSelectionController<ShelfSectionModel>,
	nestedScrollStateHandle: NestedScrollStateHandle,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()), FastScroller.SectionIndexer {

	init {
		val pool = RecyclerView.RecycledViewPool()
		delegatesManager
			.addDelegate(
				shelfGroupAD(
					sharedPool = pool,
					lifecycleOwner = lifecycleOwner,
					coil = coil,
					sizeResolver = sizeResolver,
					selectionController = selectionController,
					listener = listener,
					nestedScrollStateHandle = nestedScrollStateHandle,
				),
			)
			.addDelegate(loadingStateAD())
			.addDelegate(loadingFooterAD())
			.addDelegate(emptyHintAD(coil, lifecycleOwner, listener))
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, listener))
			.addDelegate(errorStateListAD(listener))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		val item = items.getOrNull(position) as? ShelfSectionModel ?: return null
		return item.getTitle(context.resources)
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is ShelfSectionModel && newItem is ShelfSectionModel -> {
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
				oldItem is ShelfSectionModel && newItem is ShelfSectionModel -> Unit
				else -> super.getChangePayload(oldItem, newItem)
			}
		}
	}
}
