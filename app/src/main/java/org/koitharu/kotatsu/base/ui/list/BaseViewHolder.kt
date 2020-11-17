package org.koitharu.kotatsu.base.ui.list

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.koin.core.component.KoinComponent
import org.koitharu.kotatsu.utils.ext.inflate

abstract class BaseViewHolder<T, E> protected constructor(view: View) :
	RecyclerView.ViewHolder(view), LayoutContainer, KoinComponent {

	constructor(parent: ViewGroup, @LayoutRes resId: Int) : this(parent.inflate(resId))

	override val containerView: View?
		get() = itemView

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

	fun setOnItemClickListener(listener: OnRecyclerItemClickListener<T>?) {
		val listenersAdapter = listener?.let { HolderListenersAdapter(it) }
		itemView.setOnClickListener(listenersAdapter)
		itemView.setOnLongClickListener(listenersAdapter)
	}

	open fun onRecycled() = Unit

	abstract fun onBind(data: T, extra: E)

	private inner class HolderListenersAdapter(private val listener: OnRecyclerItemClickListener<T>) :
		View.OnClickListener, View.OnLongClickListener {

		override fun onClick(v: View) {
			listener.onItemClick(boundData ?: return, bindingAdapterPosition, v)
		}

		override fun onLongClick(v: View): Boolean {
			return listener.onItemLongClick(boundData ?: return false, bindingAdapterPosition, v)
		}
	}
}