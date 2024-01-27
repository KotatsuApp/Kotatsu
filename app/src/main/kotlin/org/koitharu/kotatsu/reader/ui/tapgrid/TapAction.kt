package org.koitharu.kotatsu.reader.ui.tapgrid

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class TapAction(
	@StringRes val nameStringResId: Int,
	val color: Int,
) {

	PAGE_NEXT(R.string.next_page, 0x8BFF00),
	PAGE_PREV(R.string.prev_page, 0xFF4700),
	CHAPTER_NEXT(R.string.next_chapter, 0x327E49),
	CHAPTER_PREV(R.string.prev_chapter, 0x7E1218),
	TOGGLE_UI(R.string.toggle_ui, 0x3D69C5),
	SHOW_MENU(R.string.show_menu, 0xAA1AC5),
}
