package org.koitharu.kotatsu.ui.reader.reversed

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class ReversedPageAnimTransformer : ViewPager2.PageTransformer {

	override fun transformPage(page: View, position: Float) {
		with(page) {
			val pageWidth = width
			when {
				position > 1 -> alpha = 0f
				position >= 0 -> {
					alpha = 1f
					translationX = 0f
					translationZ = 0f
					scaleX = 1 + FACTOR * position
					scaleY = 1f
				}
				position >= -1 -> {
					alpha = 1f
					translationX = pageWidth * -position
					translationZ = -1f
					scaleX = 1f
					scaleY = 1f
				}
				else -> alpha = 0f
			}
		}
	}

	private companion object {

		const val FACTOR = 0.1f
	}
}