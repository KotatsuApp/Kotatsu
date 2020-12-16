package org.koitharu.kotatsu.base.ui.list

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.koin.core.component.KoinComponent

@Deprecated("")
abstract class BaseViewHolder<T, E, B : ViewBinding> protected constructor(val binding: B) :
	RecyclerView.ViewHolder(binding.root), KoinComponent {

	var boundData: T? = null
		private set

	val context get() = itemView.context!!

	fun bind(data: T, extra: E) {
		boundData = data
		onBind(data, extra)
	}

	fun requireData(): T {
		return boundData ?: throw IllegalStateException("Calling requireData() before bind()")
	}

	open fun onRecycled() = Unit

	abstract fun onBind(data: T, extra: E)
}