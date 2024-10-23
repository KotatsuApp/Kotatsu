package org.koitharu.kotatsu.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import coil3.Extras
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.colorSpace
import coil3.request.premultipliedAlpha
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import kotlinx.coroutines.runInterruptible
import kotlin.math.roundToInt

class RegionBitmapDecoder(
	private val source: ImageSource,
	private val options: Options,
) : Decoder {

	override suspend fun decode(): DecodeResult = runInterruptible {
		val regionDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			BitmapRegionDecoder.newInstance(source.source().inputStream())
		} else {
			@Suppress("DEPRECATION")
			BitmapRegionDecoder.newInstance(source.source().inputStream(), false)
		}
		checkNotNull(regionDecoder)
		val bitmapOptions = BitmapFactory.Options()
		try {
			val rect = bitmapOptions.configureScale(regionDecoder.width, regionDecoder.height)
			bitmapOptions.configureConfig()
			val bitmap = regionDecoder.decodeRegion(rect, bitmapOptions)
			bitmap.density = options.context.resources.displayMetrics.densityDpi
			DecodeResult(
				image = bitmap.asImage(),
				isSampled = true,
			)
		} finally {
			regionDecoder.recycle()
		}
	}

	/** Compute and set the scaling properties for [BitmapFactory.Options]. */
	private fun BitmapFactory.Options.configureScale(srcWidth: Int, srcHeight: Int): Rect {
		val dstWidth = options.size.widthPx(options.scale) { srcWidth }
		val dstHeight = options.size.heightPx(options.scale) { srcHeight }

		val srcRatio = srcWidth / srcHeight.toDouble()
		val dstRatio = dstWidth / dstHeight.toDouble()
		val rect = if (srcRatio < dstRatio) {
			// probably manga
			Rect(0, 0, srcWidth, (srcWidth / dstRatio).toInt().coerceAtLeast(1))
		} else {
			Rect(0, 0, (srcHeight / dstRatio).toInt().coerceAtLeast(1), srcHeight)
		}
		val scroll = options.getExtra(regionScrollKey)
		if (scroll == SCROLL_UNDEFINED) {
			rect.offsetTo(
				(srcWidth - rect.width()) / 2,
				(srcHeight - rect.height()) / 2,
			)
		} else {
			rect.offsetTo(
				(srcWidth - rect.width()) / 2,
				(scroll * dstRatio).toInt().coerceAtMost(srcHeight - rect.height()),
			)
		}

		// Calculate the image's sample size.
		inSampleSize = DecodeUtils.calculateInSampleSize(
			srcWidth = rect.width(),
			srcHeight = rect.height(),
			dstWidth = dstWidth,
			dstHeight = dstHeight,
			scale = options.scale,
		)

		// Calculate the image's density scaling multiple.
		var scale = DecodeUtils.computeSizeMultiplier(
			srcWidth = rect.width() / inSampleSize.toDouble(),
			srcHeight = rect.height() / inSampleSize.toDouble(),
			dstWidth = dstWidth.toDouble(),
			dstHeight = dstHeight.toDouble(),
			scale = options.scale,
		)

		// Only upscale the image if the options require an exact size.
		if (options.precision == Precision.INEXACT) {
			scale = scale.coerceAtMost(1.0)
		}

		inScaled = scale != 1.0
		if (inScaled) {
			if (scale > 1) {
				// Upscale
				inDensity = (Int.MAX_VALUE / scale).roundToInt()
				inTargetDensity = Int.MAX_VALUE
			} else {
				// Downscale
				inDensity = Int.MAX_VALUE
				inTargetDensity = (Int.MAX_VALUE * scale).roundToInt()
			}
		}
		return rect
	}

	private fun BitmapFactory.Options.configureConfig() {
		var config = options.bitmapConfig

		inMutable = false

		if (Build.VERSION.SDK_INT >= 26 && options.colorSpace != null) {
			inPreferredColorSpace = options.colorSpace
		}
		inPremultiplied = options.premultipliedAlpha

		// Decode the image as RGB_565 as an optimization if allowed.
		if (options.allowRgb565 && config == Bitmap.Config.ARGB_8888 && outMimeType == "image/jpeg") {
			config = Bitmap.Config.RGB_565
		}

		// High color depth images must be decoded as either RGBA_F16 or HARDWARE.
		if (Build.VERSION.SDK_INT >= 26 && outConfig == Bitmap.Config.RGBA_F16 && config != Bitmap.Config.HARDWARE) {
			config = Bitmap.Config.RGBA_F16
		}

		inPreferredConfig = config
	}

	object Factory : Decoder.Factory {

		override fun create(
			result: SourceFetchResult,
			options: Options,
			imageLoader: ImageLoader
		): Decoder = RegionBitmapDecoder(result.source, options)

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}

	companion object {

		const val SCROLL_UNDEFINED = -1
		val regionScrollKey = Extras.Key(SCROLL_UNDEFINED)

		private inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
			return if (isOriginal) original() else width.toPx(scale)
		}

		private inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
			return if (isOriginal) original() else height.toPx(scale)
		}

		private fun Dimension.toPx(scale: Scale) = pxOrElse {
			when (scale) {
				Scale.FILL -> Int.MIN_VALUE
				Scale.FIT -> Int.MAX_VALUE
			}
		}
	}
}
