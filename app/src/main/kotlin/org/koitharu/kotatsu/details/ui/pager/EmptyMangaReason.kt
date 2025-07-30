package org.koitharu.kotatsu.details.ui.pager

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class EmptyMangaReason(
	@StringRes val msgResId: Int,
) {

	NO_CHAPTERS(R.string.no_chapters_in_manga),
	LOADING_ERROR(R.string.chapters_load_failed),
	RESTRICTED(R.string.manga_restricted_description),
}
