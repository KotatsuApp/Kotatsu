package org.koitharu.kotatsu.reader.ui.pager.standard

import android.annotation.SuppressLint
import android.graphics.PointF
import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.ifZero
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.databinding.ItemPageBinding
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.BasePageHolder
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage

open class PageHolder(
	owner: LifecycleOwner,
	binding: ItemPageBinding,
	loader: PageLoader,
	settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : BasePageHolder<ItemPageBinding>(binding, loader, settings, networkState, exceptionResolver),
	View.OnClickListener {

	init {
		binding.ssiv.bindToLifecycle(owner)
		binding.ssiv.isEagerLoadingEnabled = !context.isLowRamDevice()
		binding.ssiv.addOnImageEventListener(delegate)
		@Suppress("LeakingThis")
		bindingInfo.buttonRetry.setOnClickListener(this)
		@Suppress("LeakingThis")
		bindingInfo.buttonErrorDetails.setOnClickListener(this)
		binding.textViewNumber.isVisible = settings.isPagesNumbersEnabled
		binding.zoomControl.listener = SsivZoomListener(binding.ssiv)
	}

	override fun onConfigChanged() {
		super.onConfigChanged()
		binding.zoomControl.isVisible = settings.isZoomControlsEnabled
	}

	@SuppressLint("SetTextI18n")
	override fun onBind(data: ReaderPage) {
		delegate.onBind(data.toMangaPage())
		binding.textViewNumber.text = (data.index + 1).toString()
	}

	override fun onRecycled() {
		super.onRecycled()
		binding.ssiv.recycle()
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
		binding.ssiv.maxScale = 2f * maxOf(
			binding.ssiv.width / binding.ssiv.sWidth.toFloat(),
			binding.ssiv.height / binding.ssiv.sHeight.toFloat(),
		)
		binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
		when (settings.zoomMode) {
			ZoomMode.FIT_CENTER -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
				binding.ssiv.resetScaleAndCenter()
			}

			ZoomMode.FIT_HEIGHT -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
				binding.ssiv.minScale = binding.ssiv.height / binding.ssiv.sHeight.toFloat()
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.minScale,
					PointF(0f, binding.ssiv.sHeight / 2f),
				)
			}

			ZoomMode.FIT_WIDTH -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
				binding.ssiv.minScale = binding.ssiv.width / binding.ssiv.sWidth.toFloat()
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.minScale,
					PointF(binding.ssiv.sWidth / 2f, 0f),
				)
			}

			ZoomMode.KEEP_START -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.maxScale,
					PointF(0f, 0f),
				)
			}
		}
	}

	override fun onImageShown() {
		bindingInfo.progressBar.hide()
	}

	final override fun onClick(v: View) {
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
}
