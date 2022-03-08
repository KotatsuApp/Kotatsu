package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.reader.domain.PageLoader

abstract class BasePageHolder<B : ViewBinding>(
	protected val binding: B,
	loader: PageLoader,
	settings: AppSettings,
	exceptionResolver: ExceptionResolver
) : RecyclerView.ViewHolder(binding.root), PageHolderDelegate.Callback {

	protected val delegate = PageHolderDelegate(loader, settings, this, exceptionResolver)

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

	@CallSuper
	open fun onRecycled() {
		delegate.onRecycle()
	}
}