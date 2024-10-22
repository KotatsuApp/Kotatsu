package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
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
import org.koitharu.kotatsu.core.image.RegionBitmapDecoder
import org.koitharu.kotatsu.core.ui.image.AnimatedPlaceholderDrawable
import org.koitharu.kotatsu.core.util.progress.ImageRequestIndicatorListener
import org.koitharu.kotatsu.parsers.model.MangaSource
import com.google.android.material.R as materialR

fun ImageView.newImageRequest(lifecycleOwner: LifecycleOwner, data: Any?): ImageRequest.Builder? {
	val current = CoilUtils.result(this)
	if (current?.request?.lifecycle === lifecycleOwner.lifecycle) {
		if (current is SuccessResult && current.request.data == data) {
			return null
		}
	}
	// disposeImageRequest()
	return ImageRequest.Builder(context)
		.data(data?.takeUnless { it == "" || it == 0 })
		.lifecycle(lifecycleOwner)
		.crossfade(context)
		.target(this)
}

fun ImageView.disposeImageRequest() {
	CoilUtils.dispose(this)
	setImageDrawable(null)
}

fun ImageRequest.Builder.enqueueWith(loader: ImageLoader) = loader.enqueue(build())

fun ImageResult.getDrawableOrThrow() = when (this) {
	is SuccessResult -> drawable
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

fun ImageRequest.Builder.indicator(indicators: List<BaseProgressIndicator<*>>): ImageRequest.Builder {
	return addListener(ImageRequestIndicatorListener(indicators))
}

fun ImageRequest.Builder.decodeRegion(
	scroll: Int = RegionBitmapDecoder.SCROLL_UNDEFINED,
): ImageRequest.Builder = setParameter(RegionBitmapDecoder.PARAM_REGION, true)
	.setParameter(RegionBitmapDecoder.PARAM_SCROLL, scroll)

@Suppress("SpellCheckingInspection")
fun ImageRequest.Builder.crossfade(context: Context): ImageRequest.Builder {
	val duration = context.resources.getInteger(R.integer.config_defaultAnimTime) * context.animatorDurationScale
	return crossfade(duration.toInt())
}

fun ImageRequest.Builder.source(source: MangaSource?): ImageRequest.Builder {
	return tag(MangaSource::class.java, source)
}

fun ImageRequest.Builder.defaultPlaceholders(context: Context): ImageRequest.Builder {
	val errorColor = ColorUtils.blendARGB(
		context.getThemeColor(materialR.attr.colorErrorContainer),
		context.getThemeColor(materialR.attr.colorBackgroundFloating),
		0.25f,
	)
	return placeholder(AnimatedPlaceholderDrawable(context))
		.fallback(ColorDrawable(context.getThemeColor(materialR.attr.colorSurfaceContainer)))
		.error(ColorDrawable(errorColor))
}

fun ImageRequest.Builder.addListener(listener: ImageRequest.Listener): ImageRequest.Builder {
	val existing = build().listener
	return listener(
		when (existing) {
			null -> listener
			is CompositeImageRequestListener -> existing + listener
			else -> CompositeImageRequestListener(arrayOf(existing, listener))
		},
	)
}

private class CompositeImageRequestListener(
	private val delegates: Array<ImageRequest.Listener>,
) : ImageRequest.Listener {

	override fun onCancel(request: ImageRequest) = delegates.forEach { it.onCancel(request) }

	override fun onError(request: ImageRequest, result: ErrorResult) = delegates.forEach { it.onError(request, result) }

	override fun onStart(request: ImageRequest) = delegates.forEach { it.onStart(request) }

	override fun onSuccess(request: ImageRequest, result: SuccessResult) =
		delegates.forEach { it.onSuccess(request, result) }

	operator fun plus(other: ImageRequest.Listener) = CompositeImageRequestListener(delegates + other)
}
