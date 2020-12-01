package org.koitharu.kotatsu.reader.ui.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder

abstract class BaseReaderAdapter(protected val pages: List<ReaderPage>) :
	RecyclerView.Adapter<BaseViewHolder<ReaderPage, Unit, *>>() {

	init {
		@Suppress("LeakingThis")
		setHasStableIds(true)
	}

	override fun onBindViewHolder(holder: BaseViewHolder<ReaderPage, Unit, *>, position: Int) {
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

	override fun getItemId(position: Int) = pages[position].id

	final override fun getItemCount() = pages.size

	final override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int
	): BaseViewHolder<ReaderPage, Unit, *> {
		return onCreateViewHolder(parent).also(this::onViewHolderCreated)
	}

	protected open fun onViewHolderCreated(holder: BaseViewHolder<ReaderPage, Unit, *>) = Unit

	protected abstract fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<ReaderPage, Unit, *>
}