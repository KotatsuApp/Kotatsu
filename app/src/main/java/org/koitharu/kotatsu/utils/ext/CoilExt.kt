package org.koitharu.kotatsu.utils.ext

import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import org.koitharu.kotatsu.core.network.CommonHeaders

@Suppress("NOTHING_TO_INLINE")
inline fun ImageView.newImageRequest(url: String) = ImageRequest.Builder(context)
	.data(url)
	.crossfade(true)
	.target(this)

@Suppress("NOTHING_TO_INLINE")
inline fun ImageRequest.Builder.enqueueWith(loader: ImageLoader) = loader.enqueue(build())

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

@Suppress("NOTHING_TO_INLINE")
inline fun ImageRequest.Builder.referer(referer: String): ImageRequest.Builder {
	return setHeader(CommonHeaders.REFERER, referer)
}