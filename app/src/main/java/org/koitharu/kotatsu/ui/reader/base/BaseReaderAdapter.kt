package org.koitharu.kotatsu.ui.reader.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

abstract class BaseReaderAdapter<E>(private val pages: GroupedList<Long, MangaPage>) :
	RecyclerView.Adapter<BaseViewHolder<MangaPage, E>>() {

	init {
		@Suppress("LeakingThis")
		setHasStableIds(true)
	}

	override fun onBindViewHolder(holder: BaseViewHolder<MangaPage, E>, position: Int) {
		val item = pages[position]
		holder.bind(item, getExtra(item, position))
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

	final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MangaPage, E> {
		return onCreateViewHolder(parent).also(this::onViewHolderCreated)
	}

	protected abstract fun getExtra(item: MangaPage, position: Int): E

	protected open fun onViewHolderCreated(holder: BaseViewHolder<MangaPage, E>) = Unit

	protected abstract fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<MangaPage, E>
}