package org.koitharu.kotatsu.search.ui.multi.adapter

import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import coil3.ImageLoader
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemListGroupBinding
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.mangaGridItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.size.ItemSizeResolver
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.search.ui.multi.SearchResultsListModel

fun searchResultsAD(
	sharedPool: RecycledViewPool,
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
	listener: OnListItemClickListener<Manga>,
	itemClickListener: OnListItemClickListener<SearchResultsListModel>,
) = adapterDelegateViewBinding<SearchResultsListModel, ListModel, ItemListGroupBinding>(
	{ layoutInflater, parent -> ItemListGroupBinding.inflate(layoutInflater, parent, false) },
) {

	binding.recyclerView.setRecycledViewPool(sharedPool)
	val adapter = ListDelegationAdapter(
		mangaGridItemAD(coil, lifecycleOwner, sizeResolver, listener),
	)
	binding.recyclerView.addItemDecoration(selectionDecoration)
	binding.recyclerView.adapter = adapter
	val spacing = context.resources.getDimensionPixelOffset(R.dimen.grid_spacing_outer)
	binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
	val eventListener = AdapterDelegateClickListenerAdapter(this, itemClickListener)
	binding.buttonMore.setOnClickListener(eventListener)

	bind {
		binding.textViewTitle.text = item.getTitle(context)
		binding.buttonMore.isVisible = item.hasMore
		adapter.items = item.list
		adapter.notifyDataSetChanged()
		binding.recyclerView.isGone = item.list.isEmpty()
		binding.textViewError.textAndVisible = item.error?.getDisplayMessage(context.resources)
	}
}
