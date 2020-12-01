package org.koitharu.kotatsu.favourites.ui.categories

import android.view.MotionEvent
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.ItemCategoryBinding

fun categoryAD(
	clickListener: OnListItemClickListener<FavouriteCategory>
) = adapterDelegateViewBinding<FavouriteCategory, FavouriteCategory, ItemCategoryBinding>(
	{ inflater, parent -> ItemCategoryBinding.inflate(inflater, parent, false) }
) {

	binding.imageViewMore.setOnClickListener {
		clickListener.onItemClick(item, it)
	}
	@Suppress("ClickableViewAccessibility")
	binding.imageViewHandle.setOnTouchListener { v, event ->
		if (event.actionMasked == MotionEvent.ACTION_DOWN) {
			clickListener.onItemLongClick(item, itemView)
		} else {
			false
		}
	}

	bind {
		binding.textViewTitle.text = item.title
	}
}