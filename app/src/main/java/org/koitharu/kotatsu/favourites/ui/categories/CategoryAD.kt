package org.koitharu.kotatsu.favourites.ui.categories

import android.view.MotionEvent
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import kotlinx.android.synthetic.main.item_category.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory

fun categoryAD(
	clickListener: OnListItemClickListener<FavouriteCategory>
) = adapterDelegateLayoutContainer<FavouriteCategory, FavouriteCategory>(R.layout.item_category) {

	imageView_more.setOnClickListener {
		clickListener.onItemClick(item, it)
	}
	@Suppress("ClickableViewAccessibility")
	imageView_handle.setOnTouchListener { v, event ->
		if (event.actionMasked == MotionEvent.ACTION_DOWN) {
			clickListener.onItemLongClick(item, itemView)
		} else {
			false
		}
	}

	bind {
		textView_title.text = item.title
	}
}