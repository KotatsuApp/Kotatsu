package org.koitharu.kotatsu.core.ui.list

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder

class AdapterDelegateClickListenerAdapter<I>(
	private val adapterDelegate: AdapterDelegateViewBindingViewHolder<out I, *>,
	private val clickListener: OnListItemClickListener<I>,
) : OnClickListener, OnLongClickListener {

	override fun onClick(v: View) {
		clickListener.onItemClick(adapterDelegate.item, v)
	}

	override fun onLongClick(v: View): Boolean {
		return clickListener.onItemLongClick(adapterDelegate.item, v)
	}
}
