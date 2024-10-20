package org.koitharu.kotatsu.core.ui.list

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.core.util.Function
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import org.koitharu.kotatsu.core.ui.OnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat

class AdapterDelegateClickListenerAdapter<I, O>(
	private val adapterDelegate: AdapterDelegateViewBindingViewHolder<out I, *>,
	private val clickListener: OnListItemClickListener<O>,
	private val itemMapper: Function<I, O>,
) : OnClickListener, OnLongClickListener, OnContextClickListenerCompat {

	override fun onClick(v: View) {
		clickListener.onItemClick(mappedItem(), v)
	}

	override fun onLongClick(v: View): Boolean {
		return clickListener.onItemLongClick(mappedItem(), v)
	}

	override fun onContextClick(v: View): Boolean {
		return clickListener.onItemContextClick(mappedItem(), v)
	}

	private fun mappedItem(): O = itemMapper.apply(adapterDelegate.item)

	fun attach() = attach(adapterDelegate.itemView)

	fun attach(itemView: View) {
		itemView.setOnClickListener(this)
		itemView.setOnLongClickListener(this)
		itemView.setOnContextClickListenerCompat(this)
	}

	companion object {

		operator fun <T> invoke(
			adapterDelegate: AdapterDelegateViewBindingViewHolder<out T, *>,
			clickListener: OnListItemClickListener<T>
		): AdapterDelegateClickListenerAdapter<T, T> = AdapterDelegateClickListenerAdapter(
			adapterDelegate = adapterDelegate,
			clickListener = clickListener,
			itemMapper = { x -> x },
		)
	}
}
