package org.koitharu.kotatsu.core.ui.util

import android.view.View
import com.google.android.material.appbar.AppBarLayout

class FadingAppbarHelper(
	private val target: View
) : AppBarLayout.OnOffsetChangedListener {

	fun setup(appBarLayout: AppBarLayout) {
		appBarLayout.addOnOffsetChangedListener(this)
	}

	override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
		val scrollRange = appBarLayout?.totalScrollRange
		if (scrollRange == null || scrollRange == 0) {
			return
		}

		target.alpha = 1f + verticalOffset / (scrollRange / 2f)
	}
}
