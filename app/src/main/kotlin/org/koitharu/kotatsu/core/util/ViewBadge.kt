package org.koitharu.kotatsu.core.util

import android.view.View
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

@OptIn(ExperimentalBadgeUtils::class)
class ViewBadge(
	private val anchor: View,
	lifecycleOwner: LifecycleOwner,
) : View.OnLayoutChangeListener, DefaultLifecycleObserver {

	private var badgeDrawable: BadgeDrawable? = null
	private var maxCharacterCount: Int = -1

	var counter: Int
		get() = badgeDrawable?.number ?: 0
		set(value) {
			val badge = badgeDrawable ?: initBadge()
			if (maxCharacterCount != 0) {
				badge.number = value
			} else {
				badge.clearNumber()
			}
			badge.isVisible = value > 0
		}

	init {
		lifecycleOwner.lifecycle.addObserver(this)
	}

	override fun onLayoutChange(
		v: View?,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int,
	) {
		val badge = badgeDrawable ?: return
		BadgeUtils.setBadgeDrawableBounds(badge, anchor, null)
	}

	override fun onDestroy(owner: LifecycleOwner) {
		super.onDestroy(owner)
		clearBadge()
	}

	fun setMaxCharacterCount(value: Int) {
		maxCharacterCount = value
		badgeDrawable?.let {
			if (value == 0) {
				it.clearNumber()
			} else {
				it.maxCharacterCount = value
			}
		}
	}

	private fun initBadge(): BadgeDrawable {
		val badge = BadgeDrawable.create(anchor.context)
		if (maxCharacterCount > 0) {
			badge.maxCharacterCount = maxCharacterCount
		}
		anchor.addOnLayoutChangeListener(this)
		BadgeUtils.attachBadgeDrawable(badge, anchor)
		badgeDrawable = badge
		return badge
	}

	private fun clearBadge() {
		val badge = badgeDrawable ?: return
		anchor.removeOnLayoutChangeListener(this)
		BadgeUtils.detachBadgeDrawable(badge, anchor)
		badgeDrawable = null
	}
}
