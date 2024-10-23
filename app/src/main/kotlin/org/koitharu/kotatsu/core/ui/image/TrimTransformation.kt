package org.koitharu.kotatsu.core.ui.image

import android.graphics.Bitmap
import androidx.core.graphics.get
import coil3.size.Size
import coil3.transform.Transformation
import org.koitharu.kotatsu.reader.domain.EdgeDetector.Companion.isColorTheSame

class TrimTransformation(
	private val tolerance: Int = 20,
) : Transformation() {

	override val cacheKey: String = "${javaClass.name}-$tolerance"

	override suspend fun transform(input: Bitmap, size: Size): Bitmap {
		var left = 0
		var top = 0
		var right = 0
		var bottom = 0

		// Left
		for (x in 0 until input.width) {
			var isColBlank = true
			val prevColor = input[x, 0]
			for (y in 1 until input.height) {
				if (!isColorTheSame(input[x, y], prevColor, tolerance)) {
					isColBlank = false
					break
				}
			}
			if (isColBlank) {
				left++
			} else {
				break
			}
		}
		if (left == input.width) {
			return input
		}
		// Right
		for (x in (left until input.width).reversed()) {
			var isColBlank = true
			val prevColor = input[x, 0]
			for (y in 1 until input.height) {
				if (!isColorTheSame(input[x, y], prevColor, tolerance)) {
					isColBlank = false
					break
				}
			}
			if (isColBlank) {
				right++
			} else {
				break
			}
		}
		// Top
		for (y in 0 until input.height) {
			var isRowBlank = true
			val prevColor = input[0, y]
			for (x in 1 until input.width) {
				if (!isColorTheSame(input[x, y], prevColor, tolerance)) {
					isRowBlank = false
					break
				}
			}
			if (isRowBlank) {
				top++
			} else {
				break
			}
		}
		// Bottom
		for (y in (top until input.height).reversed()) {
			var isRowBlank = true
			val prevColor = input[0, y]
			for (x in 1 until input.width) {
				if (!isColorTheSame(input[x, y], prevColor, tolerance)) {
					isRowBlank = false
					break
				}
			}
			if (isRowBlank) {
				bottom++
			} else {
				break
			}
		}

		return if (left != 0 || right != 0 || top != 0 || bottom != 0) {
			Bitmap.createBitmap(input, left, top, input.width - left - right, input.height - top - bottom)
		} else {
			input
		}
	}
}
