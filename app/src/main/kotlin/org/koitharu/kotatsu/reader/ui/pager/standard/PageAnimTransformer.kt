package org.koitharu.kotatsu.reader.ui.pager.standard

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class PageAnimTransformer : ViewPager2.PageTransformer {

	override fun transformPage(page: View, position: Float) = with(page) {
		translationX = -position * width
		pivotX = 0f
		pivotY = height / 2f
		cameraDistance = 20000f
		when {
			position < -1f || position > 1f -> {
				alpha = 0f
				rotationY = 0f
				translationZ = -1f
			}
			position > 0f -> {
				alpha = 1f
				rotationY = 0f
				translationZ = 0f
			}
			position <= 0f -> {
				alpha = 1f
				rotationY = 120 * position
				translationZ = 2f
			}
		}
	}
}