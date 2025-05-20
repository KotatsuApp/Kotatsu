package org.koitharu.kotatsu.core.prefs

import java.util.EnumSet

enum class ReaderControl {

	PREV_CHAPTER, NEXT_CHAPTER, SLIDER, PAGES_SHEET, SCREEN_ROTATION, SAVE_PAGE, TIMER, BOOKMARK;

	companion object {

		val DEFAULT: Set<ReaderControl> = EnumSet.of(
			PREV_CHAPTER, NEXT_CHAPTER, SLIDER, PAGES_SHEET,
		)
	}
}
