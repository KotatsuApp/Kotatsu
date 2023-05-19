package org.koitharu.kotatsu.core.ui.image

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
import androidx.annotation.WorkerThread
import coil.ImageLoader
import coil.executeBlocking
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CoilImageGetter @Inject constructor(
	@ApplicationContext private val context: Context,
	private val coil: ImageLoader,
) : Html.ImageGetter {

	@WorkerThread
	override fun getDrawable(source: String?): Drawable? {
		return coil.executeBlocking(
			ImageRequest.Builder(context)
				.data(source)
				.allowHardware(false)
				.build(),
		).drawable?.apply {
			setBounds(0, 0, intrinsicHeight, intrinsicHeight)
		}
	}
}
