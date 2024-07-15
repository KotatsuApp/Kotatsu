package org.koitharu.kotatsu.reader.ui.pager.standard

import android.annotation.SuppressLint
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.ui.widgets.ZoomControl
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
) : BasePageHolder<ItemPageBinding>(binding, loader, settings, networkState, exceptionResolver, owner),
	View.OnClickListener,
	ZoomControl.ZoomControlListener {

	init {
		binding.ssiv.bindToLifecycle(owner)
		binding.ssiv.isEagerLoadingEnabled = !context.isLowRamDevice()
		binding.ssiv.addOnImageEventListener(delegate)
		@Suppress("LeakingThis")
		bindingInfo.buttonRetry.setOnClickListener(this)
		@Suppress("LeakingThis")
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
		binding.textViewNumber.isVisible = settings.isPagesNumbersEnabled
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

	override fun onImageReady(uri: Uri, bounds: Rect?) {
		val source = ImageSource.Uri(uri)
		if (bounds != null) {
			source.region(bounds)
		}
		binding.ssiv.setImage(source)
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
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return, isFromUser = true)
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

	override fun onZoomIn() {
		scaleBy(1.2f)
	}

	override fun onZoomOut() {
		scaleBy(0.8f)
	}

	private fun scaleBy(factor: Float) {
		val ssiv = binding.ssiv
		val center = ssiv.getCenter() ?: return
		val newScale = ssiv.scale * factor
		ssiv.animateScaleAndCenter(newScale, center)?.apply {
			withDuration(ssiv.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
			withInterpolator(DecelerateInterpolator())
			start()
		}
	}
}
