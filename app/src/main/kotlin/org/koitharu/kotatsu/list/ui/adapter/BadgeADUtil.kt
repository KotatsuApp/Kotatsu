@file:androidx.annotation.OptIn(ExperimentalBadgeUtils::class)

package org.koitharu.kotatsu.list.ui.adapter

import android.view.View
import androidx.annotation.CheckResult
import androidx.cardview.widget.CardView
import androidx.core.view.doOnNextLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import org.koitharu.kotatsu.R
import com.google.android.material.R as materialR

@CheckResult
fun View.bindBadge(badge: BadgeDrawable?, counter: Int): BadgeDrawable? {
	return if (counter > 0) {
		val badgeDrawable = badge ?: initBadge(this)
		badgeDrawable.number = counter
		badgeDrawable.isVisible = true
		badgeDrawable.align(this)
		badgeDrawable
	} else {
		badge?.isVisible = false
		badge
	}
}

fun View.clearBadge(badge: BadgeDrawable?) {
	BadgeUtils.detachBadgeDrawable(badge, this)
}

private fun initBadge(anchor: View): BadgeDrawable {
	val badge = BadgeDrawable.create(anchor.context)
	val resources = anchor.resources
	badge.maxCharacterCount = resources.getInteger(R.integer.manga_badge_max_character_count)
	anchor.doOnNextLayout {
		BadgeUtils.attachBadgeDrawable(badge, it)
		badge.align(it)
	}
	return badge
}

private fun BadgeDrawable.align(anchor: View) {
	val extraOffset = if (anchor is CardView) {
		(anchor.radius / 2f).toInt()
	} else {
		anchor.resources.getDimensionPixelOffset(materialR.dimen.m3_badge_offset)
	}
	horizontalOffset = intrinsicWidth + extraOffset
	verticalOffset = intrinsicHeight + extraOffset
}
