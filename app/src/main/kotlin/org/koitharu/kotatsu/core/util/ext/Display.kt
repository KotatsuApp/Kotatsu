package org.koitharu.kotatsu.core.util.ext

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display

@Suppress("DEPRECATION")
val Activity.displayCompat: Display
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		display ?: windowManager.defaultDisplay
	} else {
		windowManager.defaultDisplay
	}

fun Activity.getDisplaySize(): Rect {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		windowManager.currentWindowMetrics.bounds
	} else {
		val dm = DisplayMetrics()
		displayCompat.getRealMetrics(dm)
		Rect(0, 0, dm.widthPixels, dm.heightPixels)
	}
}
