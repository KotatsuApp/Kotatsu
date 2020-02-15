package org.koitharu.kotatsu.ui.main.list.favourites.categories

import android.util.SparseBooleanArray
import android.view.ViewGroup
import android.widget.Checkable
import androidx.core.util.set
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class CategoriesAdapter(private val listener: OnCategoryCheckListener) :
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
		CategoryHolder(
			parent
		)

	override fun onGetItemId(item: FavouriteCategory) = item.id

	override fun onViewHolderCreated(holder: BaseViewHolder<FavouriteCategory, Boolean>) {
		super.onViewHolderCreated(holder)
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