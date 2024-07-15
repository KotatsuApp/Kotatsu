package org.koitharu.kotatsu.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.ext.use
import kotlin.math.abs

class EdgeDetector(private val context: Context) {

	private val mutex = Mutex()

	suspend fun getBounds(imageSource: ImageSource): Rect? = mutex.withLock {
		withContext(Dispatchers.IO) {
			val decoder = SkiaPooledImageRegionDecoder(Bitmap.Config.RGB_565)
			try {
				val size = runInterruptible {
					decoder.init(context, imageSource)
				}
				val edges = coroutineScope {
					listOf(
						async { detectLeftRightEdge(decoder, size, isLeft = true) },
						async { detectTopBottomEdge(decoder, size, isTop = true) },
						async { detectLeftRightEdge(decoder, size, isLeft = false) },
						async { detectTopBottomEdge(decoder, size, isTop = false) },
					).awaitAll()
				}
				var hasEdges = false
				for (edge in edges) {
					if (edge > 0) {
						hasEdges = true
					} else if (edge < 0) {
						return@withContext null
					}
				}
				if (hasEdges) {
					Rect(edges[0], edges[1], size.x - edges[2], size.y - edges[3])
				} else {
					null
				}
			} finally {
				decoder.recycle()
			}
		}
	}

	private fun detectLeftRightEdge(decoder: ImageRegionDecoder, size: Point, isLeft: Boolean): Int {
		var width = size.x
		val rectCount = size.x / BLOCK_SIZE
		val maxRect = rectCount / 3
		for (i in 0 until rectCount) {
			if (i > maxRect) {
				return -1
			}
			var dd = BLOCK_SIZE
			for (j in 0 until size.y / BLOCK_SIZE) {
				val regionX = if (isLeft) i * BLOCK_SIZE else size.x - (i + 1) * BLOCK_SIZE
				decoder.decodeRegion(region(regionX, j * BLOCK_SIZE), 1).use { bitmap ->
					for (ii in 0 until minOf(BLOCK_SIZE, dd)) {
						for (jj in 0 until BLOCK_SIZE) {
							val bi = if (isLeft) ii else BLOCK_SIZE - ii - 1
							if (bitmap[bi, jj].isNotWhite()) {
								width = minOf(width, BLOCK_SIZE * i + ii)
								dd--
								break
							}
						}
					}
				}
				if (dd == 0) {
					break
				}
			}
			if (dd < BLOCK_SIZE) {
				break // We have already found vertical field or it is not exist
			}
		}
		return width
	}

	private fun detectTopBottomEdge(decoder: ImageRegionDecoder, size: Point, isTop: Boolean): Int {
		var height = size.y
		val rectCount = size.y / BLOCK_SIZE
		val maxRect = rectCount / 3
		for (j in 0 until rectCount) {
			if (j > maxRect) {
				return -1
			}
			var dd = BLOCK_SIZE
			for (i in 0 until size.x / BLOCK_SIZE) {
				val regionY = if (isTop) j * BLOCK_SIZE else size.y - (j + 1) * BLOCK_SIZE
				decoder.decodeRegion(region(i * BLOCK_SIZE, regionY), 1).use { bitmap ->
					for (jj in 0 until minOf(BLOCK_SIZE, dd)) {
						for (ii in 0 until BLOCK_SIZE) {
							val bj = if (isTop) jj else BLOCK_SIZE - jj - 1
							if (bitmap[ii, bj].isNotWhite()) {
								height = minOf(height, BLOCK_SIZE * j + jj)
								dd--
								break
							}
						}
					}
				}
				if (dd == 0) {
					break
				}
			}
			if (dd < BLOCK_SIZE) {
				break // We have already found vertical field or it is not exist
			}
		}
		return height
	}

	companion object {

		private const val BLOCK_SIZE = 100
		private const val COLOR_TOLERANCE = 16

		fun isColorTheSame(@ColorInt a: Int, @ColorInt b: Int, tolerance: Int): Boolean {
			return abs(a.red - b.red) <= tolerance &&
				abs(a.green - b.green) <= tolerance &&
				abs(a.blue - b.blue) <= tolerance &&
				abs(a.alpha - b.alpha) <= tolerance
		}

		private fun Int.isNotWhite() = !isColorTheSame(this, Color.WHITE, COLOR_TOLERANCE)

		private fun region(x: Int, y: Int) = Rect(x, y, x + BLOCK_SIZE, y + BLOCK_SIZE)
	}
}
