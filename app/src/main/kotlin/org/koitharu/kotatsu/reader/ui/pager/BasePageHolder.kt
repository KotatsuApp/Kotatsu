package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.databinding.LayoutPageInfoBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings

abstract class BasePageHolder<B : ViewBinding>(
	protected val binding: B,
	loader: PageLoader,
	protected val settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : RecyclerView.ViewHolder(binding.root), PageHolderDelegate.Callback {

	@Suppress("LeakingThis")
	protected val delegate = PageHolderDelegate(loader, settings, this, networkState, exceptionResolver)
	protected val bindingInfo = LayoutPageInfoBinding.bind(binding.root)

	val context: Context
		get() = itemView.context

	var boundData: ReaderPage? = null
		private set

	override fun onConfigChanged() {
		settings.applyBackground(itemView)
	}

	fun requireData(): ReaderPage {
		return checkNotNull(boundData) { "Calling requireData() before bind()" }
	}

	fun bind(data: ReaderPage) {
		boundData = data
		onBind(data)
	}

	protected abstract fun onBind(data: ReaderPage)

	@CallSuper
	open fun onAttachedToWindow() {
		delegate.onAttachedToWindow()
	}

	@CallSuper
	open fun onDetachedFromWindow() {
		delegate.onDetachedFromWindow()
	}

	@CallSuper
	open fun onRecycled() {
		delegate.onRecycle()
	}
}
