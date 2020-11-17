package org.koitharu.kotatsu.favourites.ui.categories.select

import android.util.SparseBooleanArray
import android.view.ViewGroup
import android.widget.Checkable
import androidx.core.util.set
import org.koitharu.kotatsu.base.ui.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.FavouriteCategory

class CategoriesSelectAdapter(private val listener: OnCategoryCheckListener) :
	BaseRecyclerAdapter<FavouriteCategory, Boolean>() {

	private val checkedIds = SparseBooleanArray()

	fun setCheckedIds(ids: Iterable<Int>) {
		checkedIds.clear()
		ids.forEach {
			checkedIds[it] = true
		}
		notifyDataSetChanged()
	}

	override fun getExtra(item: FavouriteCategory, position: Int) =
		checkedIds.get(item.id.toInt(), false)

	override fun onCreateViewHolder(parent: ViewGroup) =
		CategoryCheckableHolder(
			parent
		)

	override fun onGetItemId(item: FavouriteCategory) = item.id

	override fun onViewDetachedFromWindow(holder: BaseViewHolder<FavouriteCategory, Boolean>) {
		holder.itemView.setOnClickListener(null)
	}

	override fun onViewAttachedToWindow(holder: BaseViewHolder<FavouriteCategory, Boolean>) {
		holder.itemView.setOnClickListener {
			if (it !is Checkable) return@setOnClickListener
			it.toggle()
			if (it.isChecked) {
				listener.onCategoryChecked(holder.requireData())
			} else {
				listener.onCategoryUnchecked(holder.requireData())
			}
		}
	}
}