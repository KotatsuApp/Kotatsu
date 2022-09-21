package org.koitharu.kotatsu.utils.ext

import android.content.ActivityNotFoundException
import android.content.res.Resources
import androidx.collection.arraySetOf
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import okio.FileNotFoundException
import org.acra.ktx.sendWithAcra
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CaughtException
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.ContentUnavailableException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.exception.ParseException

fun Throwable.getDisplayMessage(resources: Resources): String = when (this) {
	is AuthRequiredException -> resources.getString(R.string.auth_required)
	is CloudFlareProtectedException -> resources.getString(R.string.captcha_required)
	is ActivityNotFoundException,
	is UnsupportedOperationException,
	-> resources.getString(R.string.operation_not_supported)

	is UnsupportedFileException -> resources.getString(R.string.text_file_not_supported)
	is FileNotFoundException -> resources.getString(R.string.file_not_found)
	is EmptyHistoryException -> resources.getString(R.string.history_is_empty)
	is ContentUnavailableException -> message
	is ParseException -> shortMessage
	is UnknownHostException,
	is SocketTimeoutException,
	-> resources.getString(R.string.network_error)

	is WrongPasswordException -> resources.getString(R.string.wrong_password)
	is NotFoundException -> resources.getString(R.string.not_found_404)
	else -> localizedMessage
} ?: resources.getString(R.string.error_occurred)

fun Throwable.isReportable(): Boolean {
	return this is Error || this.javaClass in reportableExceptions
}

fun Throwable.report(message: String?) {
	val exception = CaughtException(this, message)
	exception.sendWithAcra()
}

private val reportableExceptions = arraySetOf<Class<*>>(
	ParseException::class.java,
	RuntimeException::class.java,
	IllegalStateException::class.java,
	IllegalArgumentException::class.java,
	ConcurrentModificationException::class.java,
	UnsupportedOperationException::class.java,
)

inline fun <R> runCatchingCancellable(block: () -> R): Result<R> {
	return try {
		Result.success(block())
	} catch (e: InterruptedException) {
		throw e
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		Result.failure(e)
	}
}