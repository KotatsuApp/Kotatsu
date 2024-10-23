package org.koitharu.kotatsu.reader.ui.colorfilter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil3.target.ImageViewTarget

class DoubleViewTarget(
	primaryView: ImageView,
	private val secondaryView: ImageView,
) : ImageViewTarget(primaryView) {

	override var drawable: Drawable?
		get() = super.drawable
		set(value) {
			super.drawable = value
			secondaryView.setImageDrawable(value?.constantState?.newDrawable())
		}
}
