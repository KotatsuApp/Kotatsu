package org.koitharu.kotatsu.core.ui.util

import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

@androidx.annotation.OptIn(ExperimentalBadgeUtils::class)
class OptionsMenuBadgeHelper(
	private val toolbar: Toolbar,
	@IdRes private val itemId: Int,
) {

	private var badge: BadgeDrawable? = null

	fun setBadgeVisible(isVisible: Boolean) {
		if (isVisible) {
			showBadge()
		} else {
			hideBadge()
		}
	}

	private fun hideBadge() {
		badge?.let {
			BadgeUtils.detachBadgeDrawable(it, toolbar, itemId)
		}
		badge = null
	}

	private fun showBadge() {
		val badgeDrawable = badge ?: BadgeDrawable.create(toolbar.context).also {
			badge = it
		}
		BadgeUtils.attachBadgeDrawable(badgeDrawable, toolbar, itemId)
	}
}
