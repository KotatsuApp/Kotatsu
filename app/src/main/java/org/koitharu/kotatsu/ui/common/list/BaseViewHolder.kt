package org.koitharu.kotatsu.ui.common.list

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.utils.ext.inflate

abstract class BaseViewHolder<T> protected constructor(view: View) :
	RecyclerView.ViewHolder(view), LayoutContainer, KoinComponent {

	constructor(parent: ViewGroup, @LayoutRes resId: Int) : this(parent.inflate(resId))

	override val containerView: View?
		get() = itemView

	protected var boundData: T? = null
		private set

	val context get() = itemView.context!!

	fun bind(data: T) {
		boundData = data
		onBind(data)
	}

	fun requireData() = boundData ?: throw IllegalStateException("Calling requireData() before bind()")

	abstract fun onBind(data: T)
}