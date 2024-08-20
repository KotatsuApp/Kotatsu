package org.koitharu.kotatsu.reader.ui.pager

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.ui.list.lifecycle.LifecycleAwareViewHolder
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.databinding.LayoutPageInfoBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.PageHolderDelegate.State
import org.koitharu.kotatsu.reader.ui.pager.webtoon.WebtoonHolder

abstract class BasePageHolder<B : ViewBinding>(
	protected val binding: B,
	loader: PageLoader,
	protected val settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	lifecycleOwner: LifecycleOwner,
) : LifecycleAwareViewHolder(binding.root, lifecycleOwner), PageHolderDelegate.Callback {

	@Suppress("LeakingThis")
	protected val delegate = PageHolderDelegate(
		loader = loader,
		readerSettings = settings,
		callback = this,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
		isWebtoon = this is WebtoonHolder,
	)
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

	override fun onResume() {
		super.onResume()
		if (delegate.state == State.ERROR && !delegate.isLoading()) {
			boundData?.let { delegate.retry(it.toMangaPage(), isFromUser = false) }
		}
	}

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

	protected fun SubsamplingScaleImageView.applyDownSampling(isForeground: Boolean) {
		downSampling = when {
			isForeground || !settings.isReaderOptimizationEnabled -> 1
			BuildConfig.DEBUG -> 32
			context.isLowRamDevice() -> 8
			else -> 4
		}
	}
}
