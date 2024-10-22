package org.koitharu.kotatsu.core.image

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
import kotlinx.coroutines.sync.Semaphore
import kotlin.math.roundToInt

class RegionBitmapDecoder(
	source: ImageSource, options: Options, parallelismLock: Semaphore
) : BaseCoilDecoder(source, options, parallelismLock) {

	override fun BitmapFactory.Options.decode(): DecodeResult {
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

	class Factory : Decoder.Factory {

		private val parallelismLock = Semaphore(DEFAULT_PARALLELISM)

		override fun create(
			result: SourceResult,
			options: Options,
			imageLoader: ImageLoader
		): Decoder? = if (options.parameters.value<Boolean>(PARAM_REGION) == true) {
			RegionBitmapDecoder(result.source, options, parallelismLock)
		} else {
			null
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()
	}

	companion object {

		const val PARAM_SCROLL = "scroll"
		const val PARAM_REGION = "region"
		const val SCROLL_UNDEFINED = -1
	}
}
