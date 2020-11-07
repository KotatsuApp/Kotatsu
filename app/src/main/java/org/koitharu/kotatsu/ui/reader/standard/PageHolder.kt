package org.koitharu.kotatsu.ui.reader.standard

import android.graphics.PointF
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.base.PageHolderDelegate
import org.koitharu.kotatsu.ui.reader.base.ReaderPage
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

open class PageHolder(parent: ViewGroup, loader: PageLoader) :
	BaseViewHolder<ReaderPage, Unit>(parent, R.layout.item_page),
	PageHolderDelegate.Callback, View.OnClickListener {

	private val delegate = PageHolderDelegate(loader, this)

	init {
		ssiv.setOnImageEventListener(delegate)
		button_retry.setOnClickListener(this)
	}

	override fun onBind(data: ReaderPage, extra: Unit) {
		delegate.onBind(data.toMangaPage())
	}

	override fun onRecycled() {
		delegate.onRecycle()
		ssiv.recycle()
	}

	override fun onLoadingStarted() {
		layout_error.isVisible = false
		progressBar.isVisible = true
		ssiv.recycle()
	}

	override fun onImageReady(uri: Uri) {
		ssiv.setImage(ImageSource.uri(uri))
	}

	override fun onImageShowing(zoom: ZoomMode) {
		ssiv.maxScale = 2f * maxOf(
			ssiv.width / ssiv.sWidth.toFloat(),
			ssiv.height / ssiv.sHeight.toFloat()
		)
		when (zoom) {
			ZoomMode.FIT_CENTER -> {
				ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
				ssiv.resetScaleAndCenter()
			}
			ZoomMode.FIT_HEIGHT -> {
				ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
				ssiv.minScale = ssiv.height / ssiv.sHeight.toFloat()
				ssiv.setScaleAndCenter(
					ssiv.minScale,
					PointF(0f, ssiv.sHeight / 2f)
				)
			}
			ZoomMode.FIT_WIDTH -> {
				ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
				ssiv.minScale = ssiv.width / ssiv.sWidth.toFloat()
				ssiv.setScaleAndCenter(
					ssiv.minScale,
					PointF(ssiv.sWidth / 2f, 0f)
				)
			}
			ZoomMode.KEEP_START -> {
				ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
				ssiv.setScaleAndCenter(
					ssiv.maxScale,
					PointF(0f, 0f)
				)
			}
		}
	}

	override fun onImageShown() {
		progressBar.isVisible = false
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return)
		}
	}

	override fun onError(e: Throwable) {
		textView_error.text = e.getDisplayMessage(context.resources)
		layout_error.isVisible = true
		progressBar.isVisible = false
	}
}