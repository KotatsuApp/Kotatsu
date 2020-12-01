package org.koitharu.kotatsu.reader.ui.reversed

import android.graphics.PointF
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.standard.PageHolder

class ReversedPageHolder(parent: ViewGroup, loader: PageLoader) : PageHolder(parent, loader) {

	override fun onImageShowing(zoom: ZoomMode) {
		with(binding.ssiv) {
			maxScale = 2f * maxOf(
				width / sWidth.toFloat(),
				height / sHeight.toFloat()
			)
			when (zoom) {
				ZoomMode.FIT_CENTER -> {
					setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
					resetScaleAndCenter()
				}
				ZoomMode.FIT_HEIGHT -> {
					setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
					minScale = height / sHeight.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth.toFloat(), sHeight / 2f)
					)
				}
				ZoomMode.FIT_WIDTH -> {
					setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
					minScale = width / sWidth.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth / 2f, 0f)
					)
				}
				ZoomMode.KEEP_START -> {
					setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
					setScaleAndCenter(
						maxScale,
						PointF(sWidth.toFloat(), 0f)
					)
				}
			}
		}
	}
}