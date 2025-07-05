package org.koitharu.kotatsu.reader.domain

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

data class ReaderColorFilter(
	val brightness: Float,
	val contrast: Float,
	val isInverted: Boolean,
	val isGrayscale: Boolean,
	val isBookBackground: Boolean,
) {

	val isEmpty: Boolean
		get() = !isGrayscale && !isInverted && !isBookBackground && brightness == 0f && contrast == 0f

	fun toColorFilter(): ColorMatrixColorFilter {
		val cm = ColorMatrix()
		if (isGrayscale) {
			cm.grayscale()
		}
		if (isInverted) {
			cm.inverted()
		}
		cm.setBrightness(brightness)
		cm.setContrast(contrast)
		if (isBookBackground) {
			cm.addBookEffect()
		}
		return ColorMatrixColorFilter(cm)
	}

	fun getBackgroundTint(): ColorStateList? = if (isBookBackground) {
		val color = Color.rgb(255, 255, (255 * BOOK_BLUE_FACTOR).toInt())
		ColorStateList.valueOf(color)
	} else {
		null
	}

	private fun ColorMatrix.setBrightness(brightness: Float) {
		val scale = brightness + 1f
		val matrix = ColorMatrix()
		matrix.setScale(scale, scale, scale, 1f)
		postConcat(matrix)
	}

	private fun ColorMatrix.setContrast(contrast: Float) {
		val scale = contrast + 1f
		val translate = (-.5f * scale + .5f) * 255f
		val array = floatArrayOf(
			scale, 0f, 0f, 0f, translate,
			0f, scale, 0f, 0f, translate,
			0f, 0f, scale, 0f, translate,
			0f, 0f, 0f, 1f, 0f,
		)
		val matrix = ColorMatrix(array)
		postConcat(matrix)
	}

	private fun ColorMatrix.inverted() {
		val matrix = floatArrayOf(
			-1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
		)
		postConcat(ColorMatrix(matrix))
	}

	private fun ColorMatrix.grayscale() {
		setSaturation(0f)
	}

	private fun ColorMatrix.addBookEffect() {
		val removeBlueMatrix = floatArrayOf(
			1f, 0f, 0f, 0f, 0f,
			0f, 1f, 0f, 0f, 0f,
			0f, 0f, BOOK_BLUE_FACTOR, 0f, 0f,
			0f, 0f, 0f, 1f, 0f,
		)
		postConcat(ColorMatrix(removeBlueMatrix))
	}

	companion object {

		private const val BOOK_BLUE_FACTOR = 0.92f

		val EMPTY = ReaderColorFilter(
			brightness = 0.0f,
			contrast = 0.0f,
			isInverted = false,
			isGrayscale = false,
			isBookBackground = false,
		)
	}
}
