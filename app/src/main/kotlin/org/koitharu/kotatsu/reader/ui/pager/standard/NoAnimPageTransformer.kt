package org.koitharu.kotatsu.reader.ui.pager.standard

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class NoAnimPageTransformer(
	private val orientation: Int
) : ViewPager2.PageTransformer {

	override fun transformPage(page: View, position: Float) {
		page.translationX = when {
			orientation != ViewPager2.ORIENTATION_HORIZONTAL -> 0f
			position in -0.5f..0.5f -> -position * page.width.toFloat()
			position > 0 -> page.width.toFloat()
			else -> -page.width.toFloat()
		}
		page.translationY = when {
			orientation != ViewPager2.ORIENTATION_VERTICAL -> 0f
			position in -0.5f..0.5f -> -position * page.height.toFloat()
			position > 0 -> page.height.toFloat()
			else -> -page.height.toFloat()
		}
	}
}
