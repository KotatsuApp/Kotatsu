package org.koitharu.kotatsu.favourites.ui.categories

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_category.*
import org.koitharu.kotatsu.base.ui.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory

class CategoriesAdapter(private val onItemClickListener: OnRecyclerItemClickListener<FavouriteCategory>) :
	BaseRecyclerAdapter<FavouriteCategory, Unit>() {

	override fun onCreateViewHolder(parent: ViewGroup) = CategoryHolder(parent)

	override fun onGetItemId(item: FavouriteCategory) = item.id

	override fun getExtra(item: FavouriteCategory, position: Int) = Unit

	@SuppressLint("ClickableViewAccessibility")
	override fun onViewAttachedToWindow(holder: BaseViewHolder<FavouriteCategory, Unit>) {
		holder.imageView_more.setOnClickListener { v ->
			onItemClickListener.onItemClick(holder.requireData(), holder.bindingAdapterPosition, v)
		}
		holder.imageView_handle.setOnTouchListener { v, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN) {
				onItemClickListener.onItemLongClick(
					holder.requireData(),
					holder.bindingAdapterPosition,
					v
				)
			} else {
				false
			}
		}
	}

	override fun onViewDetachedFromWindow(holder: BaseViewHolder<FavouriteCategory, Unit>) {
		holder.imageView_more.setOnClickListener(null)
		holder.imageView_handle.setOnTouchListener(null)
	}

	fun moveItem(oldPos: Int, newPos: Int) {
		val item = dataSet.removeAt(oldPos)
		dataSet.add(newPos, item)
		notifyItemMoved(oldPos, newPos)
	}
}