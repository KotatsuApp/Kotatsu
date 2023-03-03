package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.util.CoilUtils
import com.google.android.material.progressindicator.BaseProgressIndicator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.image.RegionBitmapDecoder
import org.koitharu.kotatsu.utils.progress.ImageRequestIndicatorListener

fun ImageView.newImageRequest(lifecycleOwner: LifecycleOwner, data: Any?): ImageRequest.Builder? {
	val current = CoilUtils.result(this)
	if (current != null && current.request.data == data) {
		return null
	}
	return ImageRequest.Builder(context)
		.data(data)
		.lifecycle(lifecycleOwner)
		.crossfade(context)
		.target(this)
}

fun ImageView.disposeImageRequest() {
	CoilUtils.dispose(this)
	setImageDrawable(null)
}

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

fun ImageRequest.Builder.indicator(indicator: BaseProgressIndicator<*>): ImageRequest.Builder {
	return listener(ImageRequestIndicatorListener(indicator))
}

fun ImageRequest.Builder.decodeRegion(): ImageRequest.Builder {
	return decoderFactory(RegionBitmapDecoder.Factory())
}

@Suppress("SpellCheckingInspection")
fun ImageRequest.Builder.crossfade(context: Context): ImageRequest.Builder {
	val duration = context.resources.getInteger(R.integer.config_defaultAnimTime) * context.animatorDurationScale
	return crossfade(duration.toInt())
}

fun ImageRequest.Builder.source(source: MangaSource?): ImageRequest.Builder {
	return tag(MangaSource::class.java, source)
}
