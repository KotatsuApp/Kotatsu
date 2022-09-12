package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.pager.BasePageHolder
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.GoneOnInvisibleListener
import org.koitharu.kotatsu.utils.ext.*

class WebtoonHolder(
	binding: ItemPageWebtoonBinding,
	loader: PageLoader,
	settings: AppSettings,
	exceptionResolver: ExceptionResolver,
) : BasePageHolder<ItemPageWebtoonBinding>(binding, loader, settings, exceptionResolver),
	View.OnClickListener {

	private var scrollToRestore = 0
	private val goneOnInvisibleListener = GoneOnInvisibleListener(bindingInfo.progressBar)

	init {
		binding.ssiv.setOnImageEventListener(delegate)
		bindingInfo.buttonRetry.setOnClickListener(this)
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
		bindingInfo.progressBar.showCompat()
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
		binding.ssiv.setImage(ImageSource.uri(uri))
	}

	override fun onImageShowing(zoom: ZoomMode) {
		with(binding.ssiv) {
			setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
			minScale = width / sWidth.toFloat()
			maxScale = minScale
			scrollTo(
				when {
					scrollToRestore != 0 -> scrollToRestore
					itemView.top < 0 -> getScrollRange()
					else -> 0
				}
			)
			scrollToRestore = 0
		}
	}

	override fun onImageShown() {
		bindingInfo.progressBar.hideCompat()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return)
		}
	}

	override fun onError(e: Throwable) {
		bindingInfo.textViewError.text = e.getDisplayMessage(context.resources)
		bindingInfo.buttonRetry.setText(
			ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again }
		)
		bindingInfo.layoutError.isVisible = true
		bindingInfo.progressBar.hideCompat()
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