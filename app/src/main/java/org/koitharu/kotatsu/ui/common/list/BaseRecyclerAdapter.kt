package org.koitharu.kotatsu.ui.common.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.utils.ext.replaceWith

abstract class BaseRecyclerAdapter<T, E>(private val onItemClickListener: OnRecyclerItemClickListener<T>? = null) :
	RecyclerView.Adapter<BaseViewHolder<T, E>>(),
	KoinComponent {

	private val dataSet = ArrayList<T>()

	init {
		@Suppress("LeakingThis")
		setHasStableIds(true)
	}

	override fun onBindViewHolder(holder: BaseViewHolder<T, E>, position: Int) {
		val item = dataSet[position]
		holder.bind(item, getExtra(item, position))
	}

	fun getItem(position: Int) = dataSet[position]

	override fun getItemId(position: Int) = onGetItemId(dataSet[position])

	protected fun findItemById(id: Long) = dataSet.find { x -> onGetItemId(x) == id }

	protected fun findItemPositionById(id: Long) =
		dataSet.indexOfFirst { x -> onGetItemId(x) == id }

	fun replaceData(newData: List<T>) {
		val updater = AdapterUpdater(dataSet, newData, this::onGetItemId)
		dataSet.replaceWith(newData)
		updater(this)
	}

	fun appendData(newData: List<T>) {
		val pos = dataSet.size
		dataSet.addAll(newData)
		notifyItemRangeInserted(pos, newData.size)
	}

	fun appendItem(newItem: T) {
		dataSet.add(newItem)
		notifyItemInserted(dataSet.lastIndex)
	}

	fun removeItem(item: T) {
		removeItemAt(dataSet.indexOf(item))
	}

	fun removeItemAt(position: Int) {
		if (position in dataSet.indices) {
			dataSet.removeAt(position)
			notifyItemRemoved(position)
		}
	}

	fun clearData() {
		dataSet.clear()
		notifyDataSetChanged()
	}

	final override fun getItemCount() = dataSet.size

	final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, E> {
		return onCreateViewHolder(parent).setOnItemClickListener(onItemClickListener)
			.also(this::onViewHolderCreated)
	}

	protected abstract fun getExtra(item: T, position: Int): E

	protected open fun onViewHolderCreated(holder: BaseViewHolder<T, E>) = Unit

	protected abstract fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<T, E>

	protected abstract fun onGetItemId(item: T): Long
}