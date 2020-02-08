package org.koitharu.kotatsu.utils.ext

import android.content.res.Resources
import kotlinx.coroutines.delay
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

suspend inline fun <T, R> T.retryUntilSuccess(maxAttempts: Int, action: T.() -> R): R {
	var attempts = maxAttempts
	while (true) {
		try {
			return this.action()
		} catch (e: Exception) {
			attempts--
			if (attempts <= 0) {
				throw e
			} else {
				delay(1000)
			}
		}
	}
}

fun Throwable.getDisplayMessage(resources: Resources) = when (this) {
	is IOException -> resources.getString(R.string.network_error)
	else -> if (BuildConfig.DEBUG) {
		message ?: resources.getString(R.string.error_occurred)
	} else {
		resources.getString(R.string.error_occurred)
	}
}