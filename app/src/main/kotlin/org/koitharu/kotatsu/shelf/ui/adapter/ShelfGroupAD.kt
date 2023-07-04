package org.koitharu.kotatsu.shelf.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.NestedScrollStateHandle
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.core.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.util.ext.removeItemDecoration
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.databinding.ItemListGroupBinding
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.size.ItemSizeResolver
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.shelf.ui.model.ShelfSectionModel

fun shelfGroupAD(
	sharedPool: RecyclerView.RecycledViewPool,
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	sizeResolver: ItemSizeResolver,
	selectionController: SectionedSelectionController<ShelfSectionModel>,
	listener: ShelfListEventListener,
	nestedScrollStateHandle: NestedScrollStateHandle,
) = adapterDelegateViewBinding<ShelfSectionModel, ListModel, ItemListGroupBinding>(
	{ layoutInflater, parent -> ItemListGroupBinding.inflate(layoutInflater, parent, false) },
) {
	val listenerAdapter = object : OnListItemClickListener<Manga>, View.OnClickListener {
		override fun onItemClick(item: Manga, view: View) {
			listener.onItemClick(item, this@adapterDelegateViewBinding.item, view)
		}

		override fun onItemLongClick(item: Manga, view: View): Boolean {
			return listener.onItemLongClick(item, this@adapterDelegateViewBinding.item, view)
		}

		override fun onClick(v: View?) {
			listener.onSectionClick(item, itemView)
		}
	}

	val adapter = AsyncListDifferDelegationAdapter(
		ListModelDiffCallback,
		mangaGridItemAD(coil, lifecycleOwner, sizeResolver, listenerAdapter),
	)
	adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
	adapter.registerAdapterDataObserver(ScrollKeepObserver(binding.recyclerView))
	binding.recyclerView.setRecycledViewPool(sharedPool)
	binding.recyclerView.adapter = adapter
	val spacingDecoration = SpacingItemDecoration(context.resources.getDimensionPixelOffset(R.dimen.grid_spacing))
	binding.recyclerView.addItemDecoration(spacingDecoration)
	binding.buttonMore.setOnClickListener(listenerAdapter)
	val stateController = nestedScrollStateHandle.attach(binding.recyclerView)

	bind {
		selectionController.attachToRecyclerView(item, binding.recyclerView)
		binding.textViewTitle.text = item.getTitle(context.resources)
		binding.buttonMore.setTextAndVisible(item.showAllButtonText)
		adapter.items = item.items
		stateController.onBind(bindingAdapterPosition)
	}

	onViewRecycled {
		stateController.onRecycled()
		adapter.items = emptyList()
		binding.recyclerView.removeItemDecoration(AbstractSelectionItemDecoration::class.java)
	}
}
