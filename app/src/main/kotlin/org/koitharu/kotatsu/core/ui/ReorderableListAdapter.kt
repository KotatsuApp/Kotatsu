package org.koitharu.kotatsu.core.ui

import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.model.ListModel
import java.util.Collections
import java.util.LinkedList

open class ReorderableListAdapter<T : ListModel> : ListDelegationAdapter<List<T>>(), FlowCollector<List<T>?> {

	private val listListeners = LinkedList<ListListener<T>>()

	override suspend fun emit(value: List<T>?) {
		val oldList = items.orEmpty()
		val newList = value.orEmpty()
		val diffResult = withContext(Dispatchers.Default) {
			val diffCallback = DiffCallback(oldList, newList)
			DiffUtil.calculateDiff(diffCallback)
		}
		super.setItems(newList)
		diffResult.dispatchUpdatesTo(this)
		listListeners.forEach { it.onCurrentListChanged(oldList, newList) }
	}

	@Deprecated("Use emit() to dispatch list updates", level = DeprecationLevel.ERROR)
	override fun setItems(items: List<T>?) {
		super.setItems(items)
	}

	fun reorderItems(oldPos: Int, newPos: Int) {
		Collections.swap(items ?: return, oldPos, newPos)
		notifyItemMoved(oldPos, newPos)
	}

	fun addDelegate(type: ListItemType, delegate: AdapterDelegate<List<T>>): ReorderableListAdapter<T> {
		delegatesManager.addDelegate(type.ordinal, delegate)
		return this
	}

	fun addListListener(listListener: ListListener<T>) {
		listListeners.add(listListener)
	}

	fun removeListListener(listListener: ListListener<T>) {
		listListeners.remove(listListener)
	}

	protected class DiffCallback<T : ListModel>(
		private val oldList: List<T>,
		private val newList: List<T>,
	) : DiffUtil.Callback() {

		override fun getOldListSize(): Int = oldList.size

		override fun getNewListSize(): Int = newList.size

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			val oldItem = oldList[oldItemPosition]
			val newItem = newList[newItemPosition]
			return newItem.areItemsTheSame(oldItem)
		}

		override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			val oldItem = oldList[oldItemPosition]
			val newItem = newList[newItemPosition]
			return newItem == oldItem
		}

		override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
			val oldItem = oldList[oldItemPosition]
			val newItem = newList[newItemPosition]
			return newItem.getChangePayload(oldItem)
		}
	}
}
