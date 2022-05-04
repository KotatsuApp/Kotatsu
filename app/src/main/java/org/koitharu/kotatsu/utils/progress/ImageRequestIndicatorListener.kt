package org.koitharu.kotatsu.utils.progress

import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.material.progressindicator.BaseProgressIndicator

class ImageRequestIndicatorListener(
	private val indicator: BaseProgressIndicator<*>,
) : ImageRequest.Listener {

	override fun onCancel(request: ImageRequest) = indicator.hide()

	override fun onError(request: ImageRequest, result: ErrorResult) = indicator.hide()

	override fun onStart(request: ImageRequest) = indicator.show()

	override fun onSuccess(request: ImageRequest, result: SuccessResult) = indicator.hide()
}