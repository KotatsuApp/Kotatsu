package org.koitharu.kotatsu.reader.ui.pager.standard

import android.graphics.PointF
import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemPageBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.pager.BasePageHolder
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.ifZero

open class PageHolder(
	binding: ItemPageBinding,
	loader: PageLoader,
	settings: AppSettings,
	exceptionResolver: ExceptionResolver,
) : BasePageHolder<ItemPageBinding>(binding, loader, settings, exceptionResolver),
	View.OnClickListener {

	init {
		binding.ssiv.setOnImageEventListener(delegate)
		binding.buttonRetry.setOnClickListener(this)
		binding.textViewNumber.isVisible = settings.isPagesNumbersEnabled
	}

	override fun onBind(data: ReaderPage) {
		delegate.onBind(data.toMangaPage())
		binding.textViewNumber.text = (data.index + 1).toString()
	}

	override fun onRecycled() {
		super.onRecycled()
		binding.ssiv.recycle()
	}

	override fun onLoadingStarted() {
		binding.layoutError.isVisible = false
		binding.progressBar.isVisible = true
		binding.ssiv.recycle()
	}

	override fun onProgressChanged(progress: Int) {
		if (progress in 0..100) {
			binding.progressBar.isIndeterminate = false
			binding.progressBar.setProgressCompat(progress, true)
		} else {
			binding.progressBar.isIndeterminate = true
		}
	}

	override fun onImageReady(uri: Uri) {
		binding.ssiv.setImage(ImageSource.uri(uri))
	}

	override fun onImageShowing(zoom: ZoomMode) {
		binding.ssiv.maxScale = 2f * maxOf(
			binding.ssiv.width / binding.ssiv.sWidth.toFloat(),
			binding.ssiv.height / binding.ssiv.sHeight.toFloat()
		)
		when (zoom) {
			ZoomMode.FIT_CENTER -> {
				binding.ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
				binding.ssiv.resetScaleAndCenter()
			}
			ZoomMode.FIT_HEIGHT -> {
				binding.ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
				binding.ssiv.minScale = binding.ssiv.height / binding.ssiv.sHeight.toFloat()
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.minScale,
					PointF(0f, binding.ssiv.sHeight / 2f)
				)
			}
			ZoomMode.FIT_WIDTH -> {
				binding.ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
				binding.ssiv.minScale = binding.ssiv.width / binding.ssiv.sWidth.toFloat()
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.minScale,
					PointF(binding.ssiv.sWidth / 2f, 0f)
				)
			}
			ZoomMode.KEEP_START -> {
				binding.ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.maxScale,
					PointF(0f, 0f)
				)
			}
		}
	}

	override fun onImageShown() {
		binding.progressBar.isVisible = false
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return)
		}
	}

	override fun onError(e: Throwable) {
		binding.textViewError.text = e.getDisplayMessage(context.resources)
		binding.buttonRetry.setText(
			ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again }
		)
		binding.layoutError.isVisible = true
		binding.progressBar.isVisible = false
	}
}