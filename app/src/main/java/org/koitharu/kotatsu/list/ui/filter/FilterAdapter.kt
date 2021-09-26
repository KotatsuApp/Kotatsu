package org.koitharu.kotatsu.list.ui.filter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

class FilterAdapter(
	private val sortOrders: List<SortOrder> = emptyList(),
	private val tags: List<MangaTag> = emptyList(),
	state: MangaFilter?,
	private val listener: OnFilterChangedListener
) : RecyclerView.Adapter<BaseViewHolder<*, Boolean, *>>() {

	private var currentState = state ?: MangaFilter(sortOrders.firstOrNull(), emptySet())

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
		VIEW_TYPE_SORT -> FilterSortHolder(parent).apply {
			itemView.setOnClickListener {
				setCheckedSort(requireData())
			}
		}
		VIEW_TYPE_TAG -> FilterTagHolder(parent).apply {
			itemView.setOnClickListener {
				setCheckedTag(boundData ?: return@setOnClickListener, !isChecked)
			}
		}
		else -> throw IllegalArgumentException("Unknown viewType $viewType")
	}

	override fun getItemCount() = sortOrders.size + tags.size

	override fun onBindViewHolder(holder: BaseViewHolder<*, Boolean, *>, position: Int) {
		when (holder) {
			is FilterSortHolder -> {
				val item = sortOrders[position]
				holder.bind(item, item == currentState.sortOrder)
			}
			is FilterTagHolder -> {
				val item = tags[position - sortOrders.size]
				holder.bind(item, item in currentState.tags)
			}
		}
	}

	override fun getItemViewType(position: Int) = when (position) {
		in sortOrders.indices -> VIEW_TYPE_SORT
		else -> VIEW_TYPE_TAG
	}

	fun setCheckedTag(tag: MangaTag, isChecked: Boolean) {
		currentState = if (tag in currentState.tags) {
			if (!isChecked) {
				currentState.copy(tags = currentState.tags - tag)
			} else {
				return
			}
		} else {
			if (isChecked) {
				currentState.copy(tags = currentState.tags + tag)
			} else {
				return
			}
		}
		val index = tags.indexOf(tag)
		if (index in tags.indices) {
			notifyItemChanged(sortOrders.size + index)
		}
		listener.onFilterChanged(currentState)
	}

	fun setCheckedSort(sort: SortOrder) {
		if (sort != currentState.sortOrder) {
			val oldItemPos = sortOrders.indexOf(currentState.sortOrder)
			val newItemPos = sortOrders.indexOf(sort)
			currentState = currentState.copy(sortOrder = sort)
			if (oldItemPos in sortOrders.indices) {
				notifyItemChanged(oldItemPos)
			}
			if (newItemPos in sortOrders.indices) {
				notifyItemChanged(newItemPos)
			}
			listener.onFilterChanged(currentState)
		}
	}

	companion object {

		const val VIEW_TYPE_SORT = 0
		const val VIEW_TYPE_TAG = 1
	}
}