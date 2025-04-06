package org.koitharu.kotatsu.reader.ui.pager.webtoon

import androidx.lifecycle.LifecycleOwner
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.GoneOnInvisibleListener
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.BasePageHolder

class WebtoonHolder(
	owner: LifecycleOwner,
	binding: ItemPageWebtoonBinding,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : BasePageHolder<ItemPageWebtoonBinding>(
	binding = binding,
	loader = loader,
	readerSettingsProducer = readerSettingsProducer,
	networkState = networkState,
	exceptionResolver = exceptionResolver,
	lifecycleOwner = owner,
) {

	override val ssiv = binding.ssiv

	private var scrollToRestore = 0
	private val goneOnInvisibleListener = GoneOnInvisibleListener(bindingInfo.progressBar)

	override fun onConfigChanged(settings: ReaderSettings) {
		super.onConfigChanged(settings)
		if (settings.applyBitmapConfig(binding.ssiv)) {
			reloadImage()
		}
		binding.ssiv.applyDownSampling(isResumed())
	}

	override fun onRecycled() {
		super.onRecycled()
		binding.ssiv.recycle()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		goneOnInvisibleListener.attach()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		goneOnInvisibleListener.detach()
	}

	override fun onReady() {
		binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
		with(binding.ssiv) {
			scrollTo(
				when {
					scrollToRestore != 0 -> scrollToRestore
					itemView.top < 0 -> getScrollRange()
					else -> 0
				},
			)
			scrollToRestore = 0
		}
	}

	fun getScrollY() = binding.ssiv.getScroll()

	fun restoreScroll(scroll: Int) {
		if (binding.ssiv.isReady) {
			binding.ssiv.scrollTo(scroll)
		} else {
			scrollToRestore = scroll
		}
	}
}
