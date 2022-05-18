package org.koitharu.kotatsu.utils.ext

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.core.content.getSystemService

val Context.displayCompat: Display?
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		display
	} else {
		@Suppress("DEPRECATION")
		getSystemService<WindowManager>()?.defaultDisplay
	}
