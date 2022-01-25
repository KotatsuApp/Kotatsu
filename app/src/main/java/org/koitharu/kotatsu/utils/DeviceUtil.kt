package org.koitharu.kotatsu.utils

import android.os.Build
import com.google.android.material.color.DynamicColors

object DeviceUtil {

	private val isSamsung by lazy {
		Build.MANUFACTURER.equals("samsung", ignoreCase = true)
	}

	val isDynamicColorAvailable by lazy {
		DynamicColors.isDynamicColorAvailable() || (isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
	}

}