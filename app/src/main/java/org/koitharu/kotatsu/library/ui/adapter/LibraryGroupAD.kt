package org.koitharu.kotatsu.library.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ItemListGroupBinding
import org.koitharu.kotatsu.library.ui.model.LibraryGroupModel
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

fun libraryGroupAD(
	sharedPool: RecyclerView.RecycledViewPool,
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
	listener: OnListItemClickListener<Manga>,
	itemClickListener: OnListItemClickListener<LibraryGroupModel>,
) = adapterDelegateViewBinding<LibraryGroupModel, ListModel, ItemListGroupBinding>(
	{ layoutInflater, parent -> ItemListGroupBinding.inflate(layoutInflater, parent, false) }
) {

	binding.recyclerView.setRecycledViewPool(sharedPool)
	val adapter = ListDelegationAdapter(
		mangaGridItemAD(coil, lifecycleOwner, listener, sizeResolver)
	)
	binding.recyclerView.addItemDecoration(selectionDecoration)
	binding.recyclerView.adapter = adapter
	val spacing = context.resources.getDimensionPixelOffset(R.dimen.grid_spacing)
	binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
	val eventListener = AdapterDelegateClickListenerAdapter(this, itemClickListener)
	itemView.setOnClickListener(eventListener)

	bind {
		binding.textViewTitle.text = item.getTitle(context.resources)
		adapter.items = item.items
		adapter.notifyDataSetChanged()
	}
}