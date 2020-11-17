package org.koitharu.kotatsu.reader.ui.reversed

import android.graphics.PointF
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page.*
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.standard.PageHolder

class ReversedPageHolder(parent: ViewGroup, loader: PageLoader) : PageHolder(parent, loader) {

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
					PointF(ssiv.sWidth.toFloat(), ssiv.sHeight / 2f)
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
					PointF(ssiv.sWidth.toFloat(), 0f)
				)
			}
		}
	}
}