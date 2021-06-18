/*https://github.com/lapism/search*/

package org.koitharu.kotatsu.base.ui.widgets.search

import android.animation.ObjectAnimator
import android.content.Context
import android.util.Property
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.content.ContextCompat

class SearchArrowDrawable constructor(context: Context) : DrawerArrowDrawable(context) {

	var position: Float
		get() = progress
		set(position) {
			progress = position
		}

	init {
		color = ContextCompat.getColor(context, android.R.color.white)
	}

	fun animate(state: Float, duration: Long) {
		val anim: ObjectAnimator = if (state == ARROW) {
			ObjectAnimator.ofFloat(
				this,
				PROGRESS,
				MENU,
				state
			)
		} else {
			ObjectAnimator.ofFloat(
				this,
				PROGRESS,
				ARROW,
				state
			)
		}
		anim.interpolator = AccelerateDecelerateInterpolator()
		anim.duration = duration
		anim.start()
	}

	companion object {

		const val MENU = 0.0f
		const val ARROW = 1.0f

		private val PROGRESS =
			object : Property<SearchArrowDrawable, Float>(Float::class.java, "progress") {
				override fun set(obj: SearchArrowDrawable, value: Float?) {
					obj.progress = value!!
				}

				override fun get(obj: SearchArrowDrawable): Float {
					return obj.progress
				}
			}
	}

}