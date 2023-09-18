package org.koitharu.kotatsu.reader.ui.pager.standard

import android.view.animation.DecelerateInterpolator
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.core.ui.widgets.ZoomControl

class SsivZoomListener(
	private val ssiv: SubsamplingScaleImageView,
) : ZoomControl.ZoomControlListener {

	override fun onZoomIn() {
		scaleBy(1.2f)
	}

	override fun onZoomOut() {
		scaleBy(0.8f)
	}

	private fun scaleBy(factor: Float) {
		val center = ssiv.getCenter() ?: return
		val newScale = ssiv.scale * factor
		ssiv.animateScaleAndCenter(newScale, center)?.apply {
			withDuration(ssiv.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
			withInterpolator(DecelerateInterpolator())
			start()
		}
	}
}
