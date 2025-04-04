package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.ImageSource
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.GoneOnInvisibleListener
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.isSerializable
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
import org.koitharu.kotatsu.parsers.util.ifZero
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.BasePageHolder
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

class WebtoonHolder(
	owner: LifecycleOwner,
	binding: ItemPageWebtoonBinding,
	loader: PageLoader,
	settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : BasePageHolder<ItemPageWebtoonBinding>(binding, loader, settings, networkState, exceptionResolver, owner),
	View.OnClickListener {

	private var scrollToRestore = 0
	private val goneOnInvisibleListener = GoneOnInvisibleListener(bindingInfo.progressBar)

	init {
		binding.ssiv.bindToLifecycle(owner)
		binding.ssiv.addOnImageEventListener(delegate)
		bindingInfo.buttonRetry.setOnClickListener(this)
		bindingInfo.buttonErrorDetails.setOnClickListener(this)
	}

	override fun onResume() {
		super.onResume()
		binding.ssiv.applyDownSampling(isForeground = true)
	}

	override fun onPause() {
		super.onPause()
		binding.ssiv.applyDownSampling(isForeground = false)
	}

	override fun onConfigChanged() {
		super.onConfigChanged()
		if (settings.applyBitmapConfig(binding.ssiv)) {
			delegate.reload()
		}
		binding.ssiv.applyDownSampling(isResumed())
	}

	override fun onBind(data: ReaderPage) {
		delegate.onBind(data.toMangaPage())
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

	override fun onLoadingStarted() {
		bindingInfo.layoutError.isVisible = false
		bindingInfo.progressBar.show()
		binding.ssiv.recycle()
		bindingInfo.textViewStatus.setTextAndVisible(R.string.loading_)
	}

	override fun onProgressChanged(progress: Int) {
		if (progress in 0..100) {
			bindingInfo.progressBar.isIndeterminate = false
			bindingInfo.progressBar.setProgressCompat(progress, true)
			bindingInfo.textViewStatus.text = context.getString(R.string.percent_string_pattern, progress.toString())
		} else {
			bindingInfo.progressBar.isIndeterminate = true
			bindingInfo.textViewStatus.setText(R.string.loading_)
		}
	}

	override fun onPreviewReady(source: ImageSource) = Unit

	override fun onImageReady(source: ImageSource) {
		binding.ssiv.setImage(source)
	}

	override fun onImageShowing(settings: ReaderSettings, isPreview: Boolean) {
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

	override fun onImageShown(isPreview: Boolean) {
		bindingInfo.progressBar.hide()
		bindingInfo.textViewStatus.isVisible = false
	}

	override fun onTrimMemory() {
		// TODO
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return, isFromUser = true)
			R.id.button_error_details -> delegate.showErrorDetails(boundData?.url)
		}
	}

	override fun onError(e: Throwable) {
		bindingInfo.textViewError.text = e.getDisplayMessage(context.resources)
		bindingInfo.buttonRetry.setText(
			ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again },
		)
		bindingInfo.buttonErrorDetails.isVisible = e.isSerializable()
		bindingInfo.layoutError.isVisible = true
		bindingInfo.progressBar.hide()
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
