package org.koitharu.kotatsu.ui.common.list

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.koin.core.KoinComponent
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

	fun requireData() = boundData ?: throw IllegalStateException("Calling requireData() before bind()")

	fun setOnItemClickListener(listener: OnRecyclerItemClickListener<T>?): BaseViewHolder<T, E> {
		if (listener != null) {
			itemView.setOnClickListener {
				listener.onItemClick(boundData ?: return@setOnClickListener, bindingAdapterPosition, it)
			}
			itemView.setOnLongClickListener {
				listener.onItemLongClick(boundData ?: return@setOnLongClickListener false, bindingAdapterPosition, it)
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				itemView.setOnContextClickListener {
					listener.onItemLongClick(boundData ?: return@setOnContextClickListener false, bindingAdapterPosition, it)
				}
			}
		}
		return this
	}

	open fun onRecycled() = Unit

	abstract fun onBind(data: T, extra: E)
}