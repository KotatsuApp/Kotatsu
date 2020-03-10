package org.koitharu.kotatsu.ui.reader.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

abstract class BaseReaderAdapter(private val pages: GroupedList<Long, MangaPage>) :
	RecyclerView.Adapter<BaseViewHolder<MangaPage, Unit>>() {

	init {
		@Suppress("LeakingThis")
		setHasStableIds(true)
	}

	override fun onBindViewHolder(holder: BaseViewHolder<MangaPage, Unit>, position: Int) {
		val item = pages[position]
		holder.bind(item, Unit)
	}

	fun getItem(position: Int) = pages[position]

	fun notifyItemsAppended(count: Int) {
		notifyItemRangeInserted(pages.size - count, count)
	}

	fun notifyItemsPrepended(count: Int) {
		notifyItemRangeInserted(0, count)
	}

	fun notifyItemsRemovedStart(count: Int) {
		notifyItemRangeRemoved(0, count)
	}

	fun notifyItemsRemovedEnd(count: Int) {
		notifyItemRangeRemoved(pages.size - count, count)
	}

	override fun getItemId(position: Int) = pages[position].id

	final override fun getItemCount() = pages.size

	final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MangaPage, Unit> {
		return onCreateViewHolder(parent).also(this::onViewHolderCreated)
	}

	protected open fun onViewHolderCreated(holder: BaseViewHolder<MangaPage, Unit>) = Unit

	protected abstract fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<MangaPage, Unit>
}