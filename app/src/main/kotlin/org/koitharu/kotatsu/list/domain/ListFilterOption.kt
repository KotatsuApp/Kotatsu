package org.koitharu.kotatsu.list.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import java.util.EnumSet

enum class ListFilterOption(
	@StringRes val titleResId: Int,
	@DrawableRes val iconResId: Int,
) {

	DOWNLOADED(R.string.on_device, R.drawable.ic_storage),
	COMPLETED(R.string.status_completed, R.drawable.ic_state_finished),
	NEW_CHAPTERS(R.string.new_chapters, R.drawable.ic_updated),
	FAVORITE(R.string.favourites, R.drawable.ic_heart_outline),
	;

	companion object {

		val HISTORY: Set<ListFilterOption> = EnumSet.of(
			DOWNLOADED,
			NEW_CHAPTERS,
			FAVORITE,
			COMPLETED,
		)
	}
}
