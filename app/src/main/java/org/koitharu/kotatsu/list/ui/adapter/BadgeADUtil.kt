@file:SuppressLint("UnsafeOptInUsageError")
package org.koitharu.kotatsu.list.ui.adapter

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.doOnNextLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import org.koitharu.kotatsu.R

fun View.bindBadge(badge: BadgeDrawable?, counter: Int): BadgeDrawable? {
	return if (counter > 0) {
		val badgeDrawable = badge ?: initBadge(this)
		badgeDrawable.number = counter
		badgeDrawable.isVisible = true
		badgeDrawable.align()
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
		badge.align()
	}
	return badge
}

private fun BadgeDrawable.align() {
	horizontalOffset = intrinsicWidth
	verticalOffset = intrinsicHeight
}