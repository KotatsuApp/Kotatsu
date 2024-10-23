package org.koitharu.kotatsu.core.util.ext

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleOwner
import coil3.Extras
import coil3.ImageLoader
import coil3.asDrawable
import coil3.fetch.FetchResult
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.lifecycle
import coil3.request.placeholder
import coil3.request.target
import coil3.size.Scale
import coil3.size.ViewSizeResolver
import coil3.toBitmap
import coil3.util.CoilUtils
import com.google.android.material.progressindicator.BaseProgressIndicator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.image.RegionBitmapDecoder
import org.koitharu.kotatsu.core.ui.image.AnimatedPlaceholderDrawable
import org.koitharu.kotatsu.core.util.progress.ImageRequestIndicatorListener
import org.koitharu.kotatsu.parsers.model.Manga
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
		.size(ViewSizeResolver(this))
		.scale(scaleType.toCoilScale())
		.target(this)
}

fun ImageView.disposeImageRequest() {
	CoilUtils.dispose(this)
	setImageDrawable(null)
}

fun ImageRequest.Builder.enqueueWith(loader: ImageLoader) = loader.enqueue(build())

fun ImageResult.getDrawableOrThrow() = when (this) {
	is SuccessResult -> image.asDrawable(request.context.resources)
	is ErrorResult -> throw throwable
}

val ImageResult.drawable: Drawable?
	get() = image?.asDrawable(request.context.resources)

fun ImageResult.toBitmapOrNull() = when (this) {
	is SuccessResult -> try {
		image.toBitmap(image.width, image.height, request.bitmapConfig)
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
): ImageRequest.Builder = apply {
	decoderFactory(RegionBitmapDecoder.Factory)
	extras[RegionBitmapDecoder.regionScrollKey] = scroll
}

@Suppress("SpellCheckingInspection")
fun ImageRequest.Builder.crossfade(context: Context): ImageRequest.Builder {
	val duration = context.resources.getInteger(R.integer.config_defaultAnimTime) * context.animatorDurationScale
	return crossfade(duration.toInt())
}

fun ImageRequest.Builder.mangaSourceExtra(source: MangaSource?): ImageRequest.Builder = apply {
	extras[mangaSourceKey] = source
}

fun ImageRequest.Builder.mangaExtra(manga: Manga): ImageRequest.Builder = apply {
	extras[mangaKey] = manga
	mangaSourceExtra(manga.source)
}

fun ImageRequest.Builder.bookmarkExtra(bookmark: Bookmark): ImageRequest.Builder = apply {
	extras[bookmarkKey] = bookmark
	mangaSourceExtra(bookmark.manga.source)
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

private fun ImageView.ScaleType.toCoilScale(): Scale = if (this == ImageView.ScaleType.CENTER_CROP) {
	Scale.FILL
} else {
	Scale.FIT
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

suspend fun ImageLoader.fetch(data: Any, options: Options): FetchResult? {
	val mappedData = components.map(data, options)
	val fetcher = components.newFetcher(mappedData, options, this)?.first
	return fetcher?.fetch()
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

val mangaKey = Extras.Key<Manga?>(null)
val bookmarkKey = Extras.Key<Bookmark?>(null)
val mangaSourceKey = Extras.Key<MangaSource?>(null)
