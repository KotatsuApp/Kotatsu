package org.koitharu.kotatsu.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.util.SynchronizedSieveCache
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class EdgeDetector(private val context: Context) {

	private val mutex = Mutex()
	private val cache = SynchronizedSieveCache<ImageSource, Rect>(CACHE_SIZE)

	suspend fun getBounds(imageSource: ImageSource): Rect? {
		cache[imageSource]?.let { rect ->
			return if (rect.isEmpty) null else rect
		}
		return mutex.withLock {
			withContext(Dispatchers.IO) {
				val decoder = SkiaPooledImageRegionDecoder(Bitmap.Config.RGB_565)
				try {
					val size = runInterruptible {
						decoder.init(context, imageSource)
					}
					val scaleFactor = calculateScaleFactor(size)
					val sampleSize = (1f / scaleFactor).toInt().coerceAtLeast(1)

					val fullBitmap = decoder.decodeRegion(
						Rect(0, 0, size.x, size.y),
						sampleSize,
					)

					try {
						val edges = coroutineScope {
							listOf(
								async { detectLeftRightEdge(fullBitmap, size, sampleSize, isLeft = true) },
								async { detectTopBottomEdge(fullBitmap, size, sampleSize, isTop = true) },
								async { detectLeftRightEdge(fullBitmap, size, sampleSize, isLeft = false) },
								async { detectTopBottomEdge(fullBitmap, size, sampleSize, isTop = false) },
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
						fullBitmap.recycle()
					}
				} finally {
					decoder.recycle()
				}
			}
		}.also {
			cache.put(imageSource, it ?: EMPTY_RECT)
		}
	}

	private fun detectLeftRightEdge(bitmap: Bitmap, size: Point, sampleSize: Int, isLeft: Boolean): Int {
		var width = size.x
		val rectCount = size.x / BLOCK_SIZE
		val maxRect = rectCount / 3
		val blockPixels = IntArray(BLOCK_SIZE * BLOCK_SIZE)

		val bitmapWidth = bitmap.width
		val bitmapHeight = bitmap.height

		for (i in 0 until rectCount) {
			if (i > maxRect) {
				return -1
			}
			var dd = BLOCK_SIZE
			for (j in 0 until size.y / BLOCK_SIZE) {
				val regionX = if (isLeft) i * BLOCK_SIZE else size.x - (i + 1) * BLOCK_SIZE
				val regionY = j * BLOCK_SIZE

				// Convert to bitmap coordinates
				val bitmapX = regionX / sampleSize
				val bitmapY = regionY / sampleSize
				val blockWidth = min(BLOCK_SIZE / sampleSize, bitmapWidth - bitmapX)
				val blockHeight = min(BLOCK_SIZE / sampleSize, bitmapHeight - bitmapY)

				if (blockWidth > 0 && blockHeight > 0) {
					bitmap.getPixels(blockPixels, 0, blockWidth, bitmapX, bitmapY, blockWidth, blockHeight)

					for (ii in 0 until minOf(blockWidth, dd / sampleSize)) {
						for (jj in 0 until blockHeight) {
							val bi = if (isLeft) ii else blockWidth - ii - 1
							val pixel = blockPixels[jj * blockWidth + bi]
							if (pixel.isNotWhite()) {
								width = minOf(width, BLOCK_SIZE * i + ii * sampleSize)
								dd -= sampleSize
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

	private fun detectTopBottomEdge(bitmap: Bitmap, size: Point, sampleSize: Int, isTop: Boolean): Int {
		var height = size.y
		val rectCount = size.y / BLOCK_SIZE
		val maxRect = rectCount / 3
		val blockPixels = IntArray(BLOCK_SIZE * BLOCK_SIZE)

		val bitmapWidth = bitmap.width
		val bitmapHeight = bitmap.height

		for (j in 0 until rectCount) {
			if (j > maxRect) {
				return -1
			}
			var dd = BLOCK_SIZE
			for (i in 0 until size.x / BLOCK_SIZE) {
				val regionX = i * BLOCK_SIZE
				val regionY = if (isTop) j * BLOCK_SIZE else size.y - (j + 1) * BLOCK_SIZE

				// Convert to bitmap coordinates
				val bitmapX = regionX / sampleSize
				val bitmapY = regionY / sampleSize
				val blockWidth = min(BLOCK_SIZE / sampleSize, bitmapWidth - bitmapX)
				val blockHeight = min(BLOCK_SIZE / sampleSize, bitmapHeight - bitmapY)

				if (blockWidth > 0 && blockHeight > 0) {
					bitmap.getPixels(blockPixels, 0, blockWidth, bitmapX, bitmapY, blockWidth, blockHeight)

					for (jj in 0 until minOf(blockHeight, dd / sampleSize)) {
						for (ii in 0 until blockWidth) {
							val bj = if (isTop) jj else blockHeight - jj - 1
							val pixel = blockPixels[bj * blockWidth + ii]
							if (pixel.isNotWhite()) {
								height = minOf(height, BLOCK_SIZE * j + jj * sampleSize)
								dd -= sampleSize
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

	/**
	 * Calculate scale factor for performance optimization.
	 * Large images can be downscaled for edge detection without losing accuracy.
	 */
	private fun calculateScaleFactor(size: Point): Float {
		val maxDimension = max(size.x, size.y)
		return when {
			maxDimension <= 1024 -> 1.0f
			maxDimension <= 2048 -> 0.75f
			maxDimension <= 4096 -> 0.5f
			else -> 0.25f
		}
	}

	companion object {

		private const val BLOCK_SIZE = 100
		private const val COLOR_TOLERANCE = 16
		private const val CACHE_SIZE = 24
		private val EMPTY_RECT = Rect(0, 0, 0, 0)

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
