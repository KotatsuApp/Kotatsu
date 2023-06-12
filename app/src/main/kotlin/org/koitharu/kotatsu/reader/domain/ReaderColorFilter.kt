package org.koitharu.kotatsu.reader.domain

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

class ReaderColorFilter(
	val brightness: Float,
	val contrast: Float,
	val isInverted: Boolean,
) {

	val isEmpty: Boolean
		get() = !isInverted && brightness == 0f && contrast == 0f

	fun toColorFilter(): ColorMatrixColorFilter {
		val cm = ColorMatrix()
		if (isInverted) {
			cm.inverted()
		}
		cm.setBrightness(brightness)
		cm.setContrast(contrast)
		return ColorMatrixColorFilter(cm)
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
		set(matrix)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ReaderColorFilter

		if (brightness != other.brightness) return false
		if (contrast != other.contrast) return false
		return isInverted == other.isInverted
	}

	override fun hashCode(): Int {
		var result = brightness.hashCode()
		result = 31 * result + contrast.hashCode()
		result = 31 * result + isInverted.hashCode()
		return result
	}
}
