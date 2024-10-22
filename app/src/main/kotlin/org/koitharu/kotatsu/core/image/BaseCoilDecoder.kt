package org.koitharu.kotatsu.core.image

import android.graphics.BitmapFactory
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.request.Options
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.annotations.Blocking

abstract class BaseCoilDecoder(
	protected val source: ImageSource,
	protected val options: Options,
	private val parallelismLock: Semaphore,
) : Decoder {

	final override suspend fun decode(): DecodeResult = parallelismLock.withPermit {
		runInterruptible { BitmapFactory.Options().decode() }
	}

	@Blocking
	protected abstract fun BitmapFactory.Options.decode(): DecodeResult

	protected companion object {

		const val DEFAULT_PARALLELISM = 4

		inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
			return if (isOriginal) original() else width.toPx(scale)
		}

		inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
			return if (isOriginal) original() else height.toPx(scale)
		}

		fun Dimension.toPx(scale: Scale) = pxOrElse {
			when (scale) {
				Scale.FILL -> Int.MIN_VALUE
				Scale.FIT -> Int.MAX_VALUE
			}
		}
	}
}
