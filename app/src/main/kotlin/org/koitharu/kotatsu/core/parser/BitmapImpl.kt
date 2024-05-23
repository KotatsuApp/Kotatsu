package org.koitharu.kotatsu.core.parser

import android.graphics.Canvas
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.Rect as AndroidRect
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.bitmap.Rect

class BitmapImpl private constructor() : Bitmap {

	lateinit var androidBitmap: AndroidBitmap

	private lateinit var canvas: Canvas

	override val height: Int
		get() = androidBitmap.height

	override val width: Int
		get() = androidBitmap.width

	override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) {
		val androidSourceBitmap = (sourceBitmap as BitmapImpl).androidBitmap

		canvas.drawBitmap(androidSourceBitmap, src.toAndroidRect(), dst.toAndroidRect(), null)
	}

	companion object {
		fun create(width: Int, height: Int): Bitmap {
			val instance = BitmapImpl()
			instance.androidBitmap = AndroidBitmap.createBitmap(width, height, AndroidBitmap.Config.ARGB_8888)
			instance.canvas = Canvas(instance.androidBitmap)

			return instance
		}

		fun create(bitmap: AndroidBitmap): Bitmap {
			val instance = BitmapImpl()
			instance.androidBitmap = bitmap.copy(AndroidBitmap.Config.ARGB_8888, true)
			instance.canvas = Canvas(instance.androidBitmap)

			return instance
		}
	}
}

private fun Rect.toAndroidRect(): AndroidRect {
	return AndroidRect(left, top, right, bottom)
}
