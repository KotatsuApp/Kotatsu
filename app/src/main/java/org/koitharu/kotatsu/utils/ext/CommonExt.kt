package org.koitharu.kotatsu.utils.ext

import android.content.res.Resources
import android.util.Log
import kotlinx.coroutines.delay
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import java.net.SocketTimeoutException

inline fun <T, R> T.safe(action: T.() -> R?) = try {
	this.action()
} catch (e: Throwable) {
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
	is UnsupportedOperationException -> resources.getString(R.string.operation_not_supported)
	is UnsupportedFileException -> resources.getString(R.string.text_file_not_supported)
	is EmptyHistoryException -> resources.getString(R.string.history_is_empty)
	is SocketTimeoutException -> resources.getString(R.string.network_error)
	is WrongPasswordException -> resources.getString(R.string.wrong_password)
	else -> message ?: resources.getString(R.string.error_occurred)
}

inline fun <T> measured(tag: String, block: () -> T): T {
	val time = System.currentTimeMillis()
	val res = block()
	val spent = System.currentTimeMillis() - time
	Log.d("measured", "$tag ${spent.format(1)} ms")
	return res
}