package org.koitharu.kotatsu.core.util.progress

import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.google.android.material.progressindicator.BaseProgressIndicator

class ImageRequestIndicatorListener(
	private val indicators: Collection<BaseProgressIndicator<*>>,
) : ImageRequest.Listener {

	override fun onCancel(request: ImageRequest) = hide()

	override fun onError(request: ImageRequest, result: ErrorResult) = hide()

	override fun onStart(request: ImageRequest) = show()

	override fun onSuccess(request: ImageRequest, result: SuccessResult) = hide()

	private fun hide() {
		indicators.forEach { it.hide() }
	}

	private fun show() {
		indicators.forEach { it.show() }
	}
}
