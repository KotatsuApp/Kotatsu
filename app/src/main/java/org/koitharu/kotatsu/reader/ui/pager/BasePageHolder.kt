package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.reader.ui.PageLoader

abstract class BasePageHolder<B : ViewBinding>(
	protected val binding: B,
	loader: PageLoader,
	settings: AppSettings
) : RecyclerView.ViewHolder(binding.root), PageHolderDelegate.Callback {

	protected val delegate = PageHolderDelegate(loader, settings, this)

	val context: Context
		get() = itemView.context

	var boundData: ReaderPage? = null
		private set

	fun requireData(): ReaderPage {
		return checkNotNull(boundData) { "Calling requireData() before bind()" }
	}

	fun bind(data: ReaderPage) {
		boundData = data
		onBind(data)
	}

	protected abstract fun onBind(data: ReaderPage)

	open fun onRecycled() = Unit
}