package org.koitharu.kotatsu.reader.ui.standard

import android.graphics.PointF
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.databinding.ItemPageBinding
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.base.PageHolderDelegate
import org.koitharu.kotatsu.reader.ui.base.ReaderPage
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

open class PageHolder(parent: ViewGroup, loader: PageLoader) :
	BaseViewHolder<ReaderPage, Unit, ItemPageBinding>(
		ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
	), PageHolderDelegate.Callback, View.OnClickListener {

	private val delegate = PageHolderDelegate(loader, this)

	init {
		binding.ssiv.setOnImageEventListener(delegate)
		binding.buttonRetry.setOnClickListener(this)
	}

	override fun onBind(data: ReaderPage, extra: Unit) {
		delegate.onBind(data.toMangaPage())
	}

	override fun onRecycled() {
		delegate.onRecycle()
		binding.ssiv.recycle()
	}

	override fun onLoadingStarted() {
		binding.layoutError.isVisible = false
		binding.progressBar.isVisible = true
		binding.ssiv.recycle()
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
		binding.layoutError.isVisible = true
		binding.progressBar.isVisible = false
	}
}