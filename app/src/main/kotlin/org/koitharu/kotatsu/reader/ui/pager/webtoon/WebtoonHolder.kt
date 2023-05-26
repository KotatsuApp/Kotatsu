package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.GoneOnInvisibleListener
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.ifZero
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
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
) : BasePageHolder<ItemPageWebtoonBinding>(binding, loader, settings, networkState, exceptionResolver),
	View.OnClickListener {

	private var scrollToRestore = 0
	private val goneOnInvisibleListener = GoneOnInvisibleListener(bindingInfo.progressBar)

	init {
		binding.ssiv.bindToLifecycle(owner)
		binding.ssiv.regionDecoderFactory = SkiaPooledImageRegionDecoder.Factory()
		binding.ssiv.addOnImageEventListener(delegate)
		bindingInfo.buttonRetry.setOnClickListener(this)
		bindingInfo.buttonErrorDetails.setOnClickListener(this)
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
	}

	override fun onProgressChanged(progress: Int) {
		if (progress in 0..100) {
			bindingInfo.progressBar.isIndeterminate = false
			bindingInfo.progressBar.setProgressCompat(progress, true)
		} else {
			bindingInfo.progressBar.isIndeterminate = true
		}
	}

	override fun onImageReady(uri: Uri) {
		binding.ssiv.setImage(ImageSource.Uri(uri))
	}

	override fun onImageShowing(settings: ReaderSettings) {
		binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
		with(binding.ssiv) {
			minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
			minScale = width / sWidth.toFloat()
			maxScale = minScale
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

	override fun onImageShown() {
		bindingInfo.progressBar.hide()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return)
			R.id.button_error_details -> delegate.showErrorDetails(boundData?.url)
		}
	}

	override fun onError(e: Throwable) {
		bindingInfo.textViewError.text = e.getDisplayMessage(context.resources)
		bindingInfo.buttonRetry.setText(
			ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again },
		)
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
