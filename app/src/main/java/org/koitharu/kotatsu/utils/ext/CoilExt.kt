package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.util.CoilUtils
import com.google.android.material.progressindicator.BaseProgressIndicator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.image.RegionBitmapDecoder
import org.koitharu.kotatsu.utils.progress.ImageRequestIndicatorListener

fun ImageView.newImageRequest(url: Any?, mangaSource: MangaSource? = null): ImageRequest.Builder? {
	val current = CoilUtils.result(this)
	if (current != null && current.request.data == url) {
		return null
	}
	return ImageRequest.Builder(context)
		.data(url)
		.tag(mangaSource)
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

fun ImageRequest.Builder.referer(referer: String): ImageRequest.Builder {
	if (referer.isEmpty()) {
		return this
	}
	try {
		setHeader(CommonHeaders.REFERER, referer)
	} catch (e: IllegalArgumentException) {
		val baseUrl = referer.baseUrl()
		if (baseUrl != null) {
			setHeader(CommonHeaders.REFERER, baseUrl)
		}
	}
	return this
}

fun ImageRequest.Builder.indicator(indicator: BaseProgressIndicator<*>): ImageRequest.Builder {
	return listener(ImageRequestIndicatorListener(indicator))
}

fun ImageRequest.Builder.decodeRegion(): ImageRequest.Builder {
	return decoderFactory(RegionBitmapDecoder.Factory())
}

@Suppress("SpellCheckingInspection")
fun ImageRequest.Builder.crossfade(context: Context?): ImageRequest.Builder {
	if (context == null) {
		crossfade(true)
		return this
	}
	val duration = context.resources.getInteger(R.integer.config_defaultAnimTime) * context.animatorDurationScale
	return crossfade(duration.toInt())
}

private fun String.baseUrl(): String? {
	return (this.toHttpUrlOrNull()?.newBuilder("/") ?: return null)
		.username("")
		.password("")
		.build()
		.toString()
}
