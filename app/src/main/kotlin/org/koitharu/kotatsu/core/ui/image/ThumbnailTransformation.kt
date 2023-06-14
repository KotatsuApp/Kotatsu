package org.koitharu.kotatsu.core.ui.image

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import coil.size.Size
import coil.size.pxOrElse
import coil.transform.Transformation

class ThumbnailTransformation : Transformation {

	override val cacheKey: String = javaClass.name

	override suspend fun transform(input: Bitmap, size: Size): Bitmap {
		return ThumbnailUtils.extractThumbnail(
			input,
			size.width.pxOrElse { input.width },
			size.height.pxOrElse { input.height },
		)
	}

	override fun equals(other: Any?) = other is ThumbnailTransformation

	override fun hashCode() = javaClass.hashCode()
}
