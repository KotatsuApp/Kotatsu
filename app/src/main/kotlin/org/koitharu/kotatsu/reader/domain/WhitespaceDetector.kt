package org.koitharu.kotatsu.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.core.graphics.get
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

class WhitespaceDetector(
	private val context: Context
) {

	private val mutex = Mutex()

	suspend fun getBounds(imageSource: ImageSource): Rect? = mutex.withLock {
		runInterruptible(Dispatchers.IO) {
			val decoder = SkiaImageRegionDecoder(Bitmap.Config.RGB_565)
			try {
				val size = decoder.init(context, imageSource)
				detectWhitespaces(decoder, size)
			} finally {
				decoder.recycle()
			}
		}
	}

	// TODO
	private fun detectWhitespaces(decoder: ImageRegionDecoder, size: Point): Rect? {
		val result = Rect(0, 0, size.x, size.y)
		val window = Rect()
		val windowSize = 200

		var baseColor = -1
		window.set(0, 0, windowSize, windowSize)
		decoder.decodeRegion(window, 1).use { bitmap ->
			baseColor = bitmap[0, 0]
			outerTop@ for (x in 1 until bitmap.width / 2) {
				for (y in 1 until bitmap.height / 2) {
					if (isSameColor(baseColor, bitmap[x, y])) {
						result.left = x
						result.top = y
					} else {
						break@outerTop
					}
				}
			}
		}
		window.set(size.x - windowSize - 1, size.y - windowSize - 1, size.x - 1, size.y - 1)
		decoder.decodeRegion(window, 1).use { bitmap ->
			outerBottom@ for (x in (bitmap.width / 2 until bitmap.width).reversed()) {
				for (y in (bitmap.height / 2 until bitmap.height).reversed()) {
					if (isSameColor(baseColor, bitmap[x, y])) {
						result.right = size.x - x
						result.bottom = size.y - y
					} else {
						break@outerBottom
					}
				}
			}
		}
		return result.takeUnless { it.isEmpty || (it.width() == size.x && it.height() == size.y) }
	}

	private fun isSameColor(a: Int, b: Int) = abs(a - b) <= 4 // TODO

	private inline fun <R> Bitmap.use(block: (Bitmap) -> R) = try {
		block(this)
	} finally {
		recycle()
	}
}
