package org.koitharu.kotatsu.reader.ui.pager.standard

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class NoAnimPageTransformer : ViewPager2.PageTransformer {

	override fun transformPage(page: View, position: Float) {
		page.translationX = when {
			position in -0.5f..0.5f -> -position * page.width.toFloat()
			position > 0 -> page.width.toFloat()
			else -> -page.width.toFloat()
		}
	}
}
