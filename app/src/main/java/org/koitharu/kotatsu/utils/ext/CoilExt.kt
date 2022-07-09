package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import com.google.android.material.progressindicator.BaseProgressIndicator
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.utils.progress.ImageRequestIndicatorListener

fun ImageView.newImageRequest(url: String?) = ImageRequest.Builder(context)
	.data(url)
	.crossfade(context)
	.target(this)

fun ImageRequest.Builder.enqueueWith(loader: ImageLoader) = loader.enqueue(build())

fun ImageResult.requireBitmap() = when (this) {
	is SuccessResult -> drawable.toBitmap()
	is ErrorResult -> throw throwable
}

fun ImageResult.toBitmapOrNull() = when (this) {
	is SuccessResult -> try {
		drawable.toBitmap()
	} catch (_: Throwable) {
		null
	}
	is ErrorResult -> null
}

fun ImageRequest.Builder.referer(referer: String): ImageRequest.Builder {
	return setHeader(CommonHeaders.REFERER, referer)
}

fun ImageRequest.Builder.indicator(indicator: BaseProgressIndicator<*>): ImageRequest.Builder {
	return listener(ImageRequestIndicatorListener(indicator))
}

fun ImageRequest.Builder.crossfade(context: Context?): ImageRequest.Builder {
	val scale = context?.animatorDurationScale ?: 1f
	return crossfade((300 * scale).toInt())
}