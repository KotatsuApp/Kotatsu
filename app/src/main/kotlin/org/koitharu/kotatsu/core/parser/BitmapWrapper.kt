package org.koitharu.kotatsu.core.parser

import android.graphics.Canvas
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.bitmap.Rect
import java.io.OutputStream
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.Rect as AndroidRect

class BitmapWrapper private constructor(
	private val androidBitmap: AndroidBitmap,
) : Bitmap {

	private val canvas by lazy { Canvas(androidBitmap) } // is not always used, so initialized lazily

	override val height: Int
		get() = androidBitmap.height

	override val width: Int
		get() = androidBitmap.width

	override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) {
		val androidSourceBitmap = (sourceBitmap as BitmapWrapper).androidBitmap
		canvas.drawBitmap(androidSourceBitmap, src.toAndroidRect(), dst.toAndroidRect(), null)
	}

	fun compressTo(output: OutputStream) {
		androidBitmap.compress(AndroidBitmap.CompressFormat.PNG, 100, output)
	}

	companion object {

		fun create(width: Int, height: Int): Bitmap = BitmapWrapper(
			AndroidBitmap.createBitmap(width, height, AndroidBitmap.Config.ARGB_8888),
		)

		fun create(bitmap: AndroidBitmap): Bitmap = BitmapWrapper(
			if (bitmap.isMutable) bitmap else bitmap.copy(AndroidBitmap.Config.ARGB_8888, true),
		)

		private fun Rect.toAndroidRect() = AndroidRect(left, top, right, bottom)
	}
}
