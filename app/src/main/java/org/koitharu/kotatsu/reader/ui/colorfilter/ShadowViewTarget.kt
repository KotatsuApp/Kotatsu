package org.koitharu.kotatsu.reader.ui.colorfilter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.target.ImageViewTarget

class ShadowViewTarget(
	view: ImageView,
	private val shadowView: ImageView,
) : ImageViewTarget(view) {

	override var drawable: Drawable?
		get() = super.drawable
		set(value) {
			super.drawable = value
			shadowView.setImageDrawable(value?.constantState?.newDrawable())
		}
}
