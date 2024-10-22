package org.koitharu.kotatsu.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import com.davemorrissey.labs.subscaleview.decoder.ImageDecodeException
import kotlinx.coroutines.sync.Semaphore
import org.aomedia.avif.android.AvifDecoder
import org.aomedia.avif.android.AvifDecoder.Info
import org.koitharu.kotatsu.core.util.ext.toByteBuffer

class AvifImageDecoder(source: ImageSource, options: Options, parallelismLock: Semaphore) :
	BaseCoilDecoder(source, options, parallelismLock) {

	override fun BitmapFactory.Options.decode(): DecodeResult {
		val bytes = source.source().use {
			it.inputStream().toByteBuffer()
		}
		val info = Info()
		if (!AvifDecoder.getInfo(bytes, bytes.remaining(), info)) {
			throw ImageDecodeException(
				null,
				"avif",
				"Requested to decode byte buffer which cannot be handled by AvifDecoder",
			)
		}
		val config = if (info.depth == 8 || info.alphaPresent) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
		val bitmap = Bitmap.createBitmap(info.width, info.height, config)
		if (!AvifDecoder.decode(bytes, bytes.remaining(), bitmap)) {
			bitmap.recycle()
			throw ImageDecodeException(null, "avif")
		}
		return DecodeResult(
			drawable = bitmap.toDrawable(options.context.resources),
			isSampled = false,
		)
	}

	class Factory : Decoder.Factory {

		private val parallelismLock = Semaphore(DEFAULT_PARALLELISM)

		override fun create(
			result: SourceResult,
			options: Options,
			imageLoader: ImageLoader
		): Decoder? = if (isApplicable(result)) {
			AvifImageDecoder(result.source, options, parallelismLock)
		} else {
			null
		}

		override fun equals(other: Any?) = other is Factory

		override fun hashCode() = javaClass.hashCode()

		private fun isApplicable(result: SourceResult): Boolean {
			return result.mimeType == "image/avif"
		}
	}
}
