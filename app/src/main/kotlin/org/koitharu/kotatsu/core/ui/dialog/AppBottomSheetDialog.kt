package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog

class AppBottomSheetDialog(context: Context, theme: Int) : BottomSheetDialog(context, theme) {

	/**
	 * https://github.com/material-components/material-components-android/issues/2582
	 */
	@Suppress("DEPRECATION")
	override fun onAttachedToWindow() {
		val window = window
		val initialSystemUiVisibility = window?.decorView?.systemUiVisibility ?: 0
		super.onAttachedToWindow()
		if (window != null) {
			// If the navigation bar is translucent at all, the BottomSheet should be edge to edge
			val drawEdgeToEdge = edgeToEdgeEnabled && Color.alpha(window.navigationBarColor) < 0xFF
			if (drawEdgeToEdge) {
				// Copied from super.onAttachedToWindow:
				val edgeToEdgeFlags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				// Fix super-class's window flag bug by respecting the initial system UI visibility:
				window.decorView.systemUiVisibility = edgeToEdgeFlags or initialSystemUiVisibility
			}
		}
	}
}
