package org.koitharu.kotatsu.core.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.DecodeUtils
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.roundToInt

class RegionBitmapDecoder(
	private val source: ImageSource,
	private val options: Options,
	private val parallelismLock: Semaphore,
) : Decoder {

	override suspend fun decode() = parallelismLock.withPermit {
		runInterruptible { BitmapFactory.Options().decode() }
	}

	private fun BitmapFactory.Options.decode(): DecodeResult {
		val regionDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			BitmapRegionDecoder.newInstance(source.source().inputStream())
		} else {
			@Suppress("DEPRECATION")
			BitmapRegionDecoder.newInstance(source.source().inputStream(), false)
		}
		checkNotNull(regionDecoder)
		try {
			val rect = configureScale(regionDecoder.width, regionDecoder.height)
			configureConfig()
			val bitmap = regionDecoder.decodeRegion(rect, this)
			bitmap.density = options.context.resources.displayMetrics.densityDpi
			return DecodeResult(
				drawable = bitmap.toDrawable(options.context.resources),
				isSampled = true,
			)
		} finally {
			regionDecoder.recycle()
		}
	}

	private fun BitmapFactory.Options.configureConfig() {
		var config = options.config

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
		val scroll = options.parameters.value(PARAM_SCROLL) ?: SCROLL_UNDEFINED
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
		if (options.allowInexactSize) {
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

	class Factory(
		maxParallelism: Int = DEFAULT_MAX_PARALLELISM,
	) : Decoder.Factory {

		@Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
		@SinceKotlin("999.9") // Only public in Java.
		constructor() : this()

		private val parallelismLock = Semaphore(maxParallelism)

		override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder {
			return RegionBitmapDecoder(result.source, options, parallelismLock)
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}

	companion object {

		const val PARAM_SCROLL = "scroll"
		const val SCROLL_UNDEFINED = -1
		private const val DEFAULT_MAX_PARALLELISM = 4

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
