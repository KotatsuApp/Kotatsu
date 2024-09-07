package org.koitharu.kotatsu.core.ui.util

import android.view.View
import com.google.android.material.appbar.AppBarLayout

class FadingAppbarMediator(
	private val appBarLayout: AppBarLayout,
	private val target: View
) : AppBarLayout.OnOffsetChangedListener {

	private var isBound: Boolean = false

	fun bind() {
		if (!isBound) {
			appBarLayout.addOnOffsetChangedListener(this)
			isBound = true
		}
	}

	fun unbind() {
		if (isBound) {
			appBarLayout.removeOnOffsetChangedListener(this)
			isBound = false
		}
		target.alpha = 1f
	}

	override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
		val scrollRange = (appBarLayout ?: return).totalScrollRange
		if (scrollRange <= 0) {
			return
		}

		target.alpha = 1f + verticalOffset / (scrollRange / 2f)
	}
}
