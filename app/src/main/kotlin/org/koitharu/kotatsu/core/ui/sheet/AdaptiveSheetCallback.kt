package org.koitharu.kotatsu.core.ui.sheet

import android.view.View

interface AdaptiveSheetCallback {

	/**
	 * Called when the sheet changes its state.
	 *
	 * @param sheet The sheet view.
	 * @param newState The new state.
	 */
	fun onStateChanged(sheet: View, newState: Int)

	/**
	 * Called when the sheet is being dragged.
	 *
	 * @param sheet The sheet view.
	 * @param slideOffset The new offset of this sheet.
	 */
	fun onSlide(sheet: View, slideOffset: Float) = Unit
}
