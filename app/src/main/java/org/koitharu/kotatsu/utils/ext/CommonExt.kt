package org.koitharu.kotatsu.utils.ext

import android.content.res.Resources
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import java.io.IOException

inline fun <T, R> T.safe(action: T.() -> R?) = try {
	this.action()
} catch (e: Exception) {
	if (BuildConfig.DEBUG) {
		e.printStackTrace()
	}
	null
}

fun Throwable.getDisplayMessage(resources: Resources) = when(this) {
	is IOException -> resources.getString(R.string.network_error)
	else -> if (BuildConfig.DEBUG) {
		message ?: resources.getString(R.string.error_occurred)
	} else {
		resources.getString(R.string.error_occurred)
	}
}