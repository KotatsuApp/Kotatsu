package org.koitharu.kotatsu.core.ui.image

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.GravityInt
import coil3.target.GenericViewTarget

class TextViewTarget(
	override val view: TextView,
	@GravityInt compoundDrawable: Int,
) : GenericViewTarget<TextView>() {

	private val drawableIndex: Int = when (compoundDrawable) {
		Gravity.START -> 0
		Gravity.TOP -> 2
		Gravity.END -> 3
		Gravity.BOTTOM -> 4
		else -> -1
	}

	override var drawable: Drawable?
		get() = if (drawableIndex != -1) {
			view.compoundDrawablesRelative[drawableIndex]
		} else {
			null
		}
		set(value) {
			if (drawableIndex == -1) {
				return
			}
			val drawables = view.compoundDrawablesRelative
			drawables[drawableIndex] = value
			view.setCompoundDrawablesRelativeWithIntrinsicBounds(
				drawables[0],
				drawables[1],
				drawables[2],
				drawables[3],
			)
		}
}
