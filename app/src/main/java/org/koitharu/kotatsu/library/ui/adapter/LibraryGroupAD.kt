package org.koitharu.kotatsu.library.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.SectionedSelectionController
import org.koitharu.kotatsu.base.ui.list.decor.AbstractSelectionItemDecoration
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ItemListGroupBinding
import org.koitharu.kotatsu.library.ui.model.LibrarySectionModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.removeItemDecoration
import org.koitharu.kotatsu.utils.ext.setTextAndVisible

fun libraryGroupAD(
	sharedPool: RecyclerView.RecycledViewPool,
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	sizeResolver: ItemSizeResolver,
	selectionController: SectionedSelectionController<LibrarySectionModel>,
	listener: LibraryListEventListener,
) = adapterDelegateViewBinding<LibrarySectionModel, ListModel, ItemListGroupBinding>(
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
		MangaItemDiffCallback(),
		mangaGridItemAD(coil, lifecycleOwner, listenerAdapter, sizeResolver),
	)
	adapter.registerAdapterDataObserver(ScrollKeepObserver(binding.recyclerView))
	binding.recyclerView.setRecycledViewPool(sharedPool)
	binding.recyclerView.adapter = adapter
	val spacingDecoration = SpacingItemDecoration(context.resources.getDimensionPixelOffset(R.dimen.grid_spacing))
	binding.recyclerView.addItemDecoration(spacingDecoration)
	binding.buttonMore.setOnClickListener(listenerAdapter)

	bind { payloads ->
		if (payloads.isEmpty()) {
			binding.recyclerView.removeItemDecoration(AbstractSelectionItemDecoration::class.java)
			selectionController.attachToRecyclerView(item, binding.recyclerView)
		}
		binding.textViewTitle.text = item.getTitle(context.resources)
		binding.buttonMore.setTextAndVisible(item.showAllButtonText)
		adapter.items = item.items
	}

	onViewRecycled {
		adapter.items = emptyList()
		binding.recyclerView.removeItemDecoration(AbstractSelectionItemDecoration::class.java)
	}
}
