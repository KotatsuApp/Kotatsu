package org.koitharu.kotatsu.scrobbling.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class ScrobblerService(
	val id: Int,
	@StringRes val titleResId: Int,
	@DrawableRes val iconResId: Int,
) {

	SHIKIMORI(1, R.string.shikimori, R.drawable.ic_shikimori)
}