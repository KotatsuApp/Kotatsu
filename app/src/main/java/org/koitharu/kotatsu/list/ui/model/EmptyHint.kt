package org.koitharu.kotatsu.list.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class EmptyHint(
	@DrawableRes icon: Int,
	@StringRes textPrimary: Int,
	@StringRes textSecondary: Int,
	@StringRes actionStringRes: Int,
) : EmptyState(icon, textPrimary, textSecondary, actionStringRes) {

	fun toState() = EmptyState(icon, textPrimary, textSecondary, actionStringRes)
}
