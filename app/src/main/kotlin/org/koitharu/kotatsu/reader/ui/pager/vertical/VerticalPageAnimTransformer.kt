package org.koitharu.kotatsu.reader.ui.pager.vertical

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class VerticalPageAnimTransformer : ViewPager2.PageTransformer {

	override fun transformPage(page: View, position: Float) = with(page) {
		translationY = -position * height
		pivotX = width / 2f
		pivotY = height * 0.2f
		cameraDistance = 20000f
		when {
			position < -1f || position > 1f -> {
				alpha = 0f
				rotationX = 0f
				translationZ = -1f
			}
			position > 0f -> {
				alpha = 1f
				rotationX = 0f
				translationZ = 0f
			}
			position <= 0f -> {
				alpha = 1f
				rotationX = -120 * position
				translationZ = 2f
			}
		}
	}
}
