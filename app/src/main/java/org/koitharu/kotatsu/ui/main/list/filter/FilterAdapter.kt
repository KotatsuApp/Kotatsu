package org.koitharu.kotatsu.ui.main.list.filter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import java.util.*
import kotlin.collections.ArrayList

class FilterAdapter(
	sortOrders: List<SortOrder> = emptyList(),
	tags: List<MangaTag> = emptyList(),
	state: MangaFilter?,
	private val listener: OnFilterChangedListener
) : RecyclerView.Adapter<BaseViewHolder<*, Boolean>>() {

	private val sortOrders = ArrayList<SortOrder>(sortOrders)
	private val tags = ArrayList(Collections.singletonList(null) + tags)

	private var currentState = state ?: MangaFilter(sortOrders.first(), null)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
		VIEW_TYPE_SORT -> FilterSortHolder(parent).apply {
			itemView.setOnClickListener {
				setCheckedSort(requireData())
			}
		}
		VIEW_TYPE_TAG -> FilterTagHolder(parent).apply {
			itemView.setOnClickListener {
				setCheckedTag(boundData)
			}
		}
		else -> throw IllegalArgumentException("Unknown viewType $viewType")
	}

	override fun getItemCount() = sortOrders.size + tags.size

	override fun onBindViewHolder(holder: BaseViewHolder<*, Boolean>, position: Int) {
		when (holder) {
			is FilterSortHolder -> {
				val item = sortOrders[position]
				holder.bind(item, item == currentState.sortOrder)
			}
			is FilterTagHolder -> {
				val item = tags[position - sortOrders.size]
				holder.bind(item, item == currentState.tag)
			}
		}
	}

	override fun getItemViewType(position: Int) = when (position) {
		in sortOrders.indices -> VIEW_TYPE_SORT
		else -> VIEW_TYPE_TAG
	}

	fun setCheckedTag(tag: MangaTag?) {
		if (tag != currentState.tag) {
			val oldItemPos = tags.indexOf(currentState.tag)
			val newItemPos = tags.indexOf(tag)
			currentState = currentState.copy(tag = tag)
			if (oldItemPos in tags.indices) {
				notifyItemChanged(sortOrders.size + oldItemPos)
			}
			if (newItemPos in tags.indices) {
				notifyItemChanged(sortOrders.size + newItemPos)
			}
			listener.onFilterChanged(currentState)
		}
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