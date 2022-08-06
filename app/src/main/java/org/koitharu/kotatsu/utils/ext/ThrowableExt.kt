package org.koitharu.kotatsu.utils.ext

import android.content.ActivityNotFoundException
import android.content.res.Resources
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okio.FileNotFoundException
import org.acra.ktx.sendWithAcra
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.*
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.ContentUnavailableException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.exception.ParseException

fun Throwable.getDisplayMessage(resources: Resources): String = when (this) {
	is AuthRequiredException -> resources.getString(R.string.auth_required)
	is CloudFlareProtectedException -> resources.getString(R.string.captcha_required)
	is ActivityNotFoundException,
	is UnsupportedOperationException, -> resources.getString(R.string.operation_not_supported)
	is UnsupportedFileException -> resources.getString(R.string.text_file_not_supported)
	is FileNotFoundException -> resources.getString(R.string.file_not_found)
	is EmptyHistoryException -> resources.getString(R.string.history_is_empty)
	is SyncApiException,
	is ContentUnavailableException, -> message
	is ParseException -> shortMessage
	is UnknownHostException,
	is SocketTimeoutException, -> resources.getString(R.string.network_error)
	is WrongPasswordException -> resources.getString(R.string.wrong_password)
	is NotFoundException -> resources.getString(R.string.not_found_404)
	else -> localizedMessage
} ?: resources.getString(R.string.error_occurred)

fun Throwable.isReportable(): Boolean {
	if (this !is Exception) {
		return true
	}
	return this is ParseException || this is IllegalArgumentException ||
		this is IllegalStateException || this.javaClass == RuntimeException::class.java
}

fun Throwable.report(message: String?) {
	CaughtException(this, message).sendWithAcra()
}
