package org.koitharu.kotatsu.ui.reader.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder

abstract class BaseReaderAdapter(protected val pages: GroupedList<Long, MangaPage>) :
	RecyclerView.Adapter<BaseViewHolder<MangaPage, Unit>>() {

	init {
		@Suppress("LeakingThis")
		setHasStableIds(true)
	}

	override fun onBindViewHolder(holder: BaseViewHolder<MangaPage, Unit>, position: Int) {
		val item = pages[position]
		holder.bind(item, Unit)
	}

	open fun getItem(position: Int) = pages[position]

	open fun notifyItemsAppended(count: Int) {
		notifyItemRangeInserted(pages.size - count, count)
	}

	open fun notifyItemsPrepended(count: Int) {
		notifyItemRangeInserted(0, count)
	}

	open fun notifyItemsRemovedStart(count: Int) {
		notifyItemRangeRemoved(0, count)
	}

	open fun notifyItemsRemovedEnd(count: Int) {
		notifyItemRangeRemoved(pages.size - count, count)
	}

	open override fun getItemId(position: Int) = pages[position].id

	final override fun getItemCount() = pages.size

	final override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int
	): BaseViewHolder<MangaPage, Unit> {
		return onCreateViewHolder(parent).also(this::onViewHolderCreated)
	}

	protected open fun onViewHolderCreated(holder: BaseViewHolder<MangaPage, Unit>) = Unit

	protected abstract fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<MangaPage, Unit>
}