package org.koitharu.kotatsu.utils

import android.app.PendingIntent
import android.os.Build

object PendingIntentCompat {

	val FLAG_IMMUTABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		PendingIntent.FLAG_IMMUTABLE
	} else {
		0
	}

	val FLAG_MUTABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		PendingIntent.FLAG_MUTABLE
	} else {
		0
	}
}