package org.koitharu.kotatsu.ui.common.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import okhttp3.internal.toImmutableList
import org.koin.core.component.KoinComponent
import org.koitharu.kotatsu.utils.ext.replaceWith

abstract class BaseRecyclerAdapter<T, E>(private val onItemClickListener: OnRecyclerItemClickListener<T>? = null) :
	RecyclerView.Adapter<BaseViewHolder<T, E>>(),
	KoinComponent {

	protected val dataSet = ArrayList<T>() //TODO make private

	val items get() = dataSet.toImmutableList()

	val hasItems get() = dataSet.isNotEmpty()

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
		onDataSetChanged()
	}

	fun appendData(newData: List<T>) {
		val pos = dataSet.size
		dataSet.addAll(newData)
		notifyItemRangeInserted(pos, newData.size)
		onDataSetChanged()
	}

	fun prependData(newData: List<T>) {
		dataSet.addAll(0, newData)
		notifyItemRangeInserted(0, newData.size)
		onDataSetChanged()
	}

	fun appendItem(newItem: T) {
		dataSet.add(newItem)
		notifyItemInserted(dataSet.lastIndex)
		onDataSetChanged()
	}

	fun removeItem(item: T) {
		removeItemAt(dataSet.indexOf(item))
		onDataSetChanged()
	}

	fun removeItemAt(position: Int) {
		if (position in dataSet.indices) {
			dataSet.removeAt(position)
			notifyItemRemoved(position)
		}
		onDataSetChanged()
	}

	fun clearData() {
		dataSet.clear()
		notifyDataSetChanged()
		onDataSetChanged()
	}

	override fun onViewRecycled(holder: BaseViewHolder<T, E>) {
		holder.onRecycled()
	}

	final override fun getItemCount() = dataSet.size

	final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, E> {
		return onCreateViewHolder(parent)
	}

	override fun onViewDetachedFromWindow(holder: BaseViewHolder<T, E>) {
		holder.setOnItemClickListener(null)
		super.onViewDetachedFromWindow(holder)
	}

	override fun onViewAttachedToWindow(holder: BaseViewHolder<T, E>) {
		super.onViewAttachedToWindow(holder)
		holder.setOnItemClickListener(onItemClickListener)
	}

	protected open fun onDataSetChanged() = Unit

	protected abstract fun getExtra(item: T, position: Int): E

	protected abstract fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<T, E>

	protected abstract fun onGetItemId(item: T): Long
}