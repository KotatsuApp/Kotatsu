package org.koitharu.kotatsu.core.util.ext

import android.content.ActivityNotFoundException
import android.content.res.Resources
import android.util.AndroidRuntimeException
import androidx.collection.arraySetOf
import okio.FileNotFoundException
import okio.IOException
import org.acra.ktx.sendWithAcra
import org.json.JSONException
import org.jsoup.HttpStatusException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.CaughtException
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.exceptions.SyncApiException
import org.koitharu.kotatsu.core.exceptions.TooManyRequestExceptions
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.ContentUnavailableException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.exception.ParseException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val MSG_NO_SPACE_LEFT = "No space left on device"

fun Throwable.getDisplayMessage(resources: Resources): String = when (this) {
	is AuthRequiredException -> resources.getString(R.string.auth_required)
	is CloudFlareProtectedException -> resources.getString(R.string.captcha_required)
	is ActivityNotFoundException,
	is UnsupportedOperationException,
	-> resources.getString(R.string.operation_not_supported)

	is TooManyRequestExceptions -> resources.getString(R.string.too_many_requests_message)
	is UnsupportedFileException -> resources.getString(R.string.text_file_not_supported)
	is FileNotFoundException -> resources.getString(R.string.file_not_found)
	is AccessDeniedException -> resources.getString(R.string.no_access_to_file)
	is EmptyHistoryException -> resources.getString(R.string.history_is_empty)
	is SyncApiException,
	is ContentUnavailableException,
	-> message

	is ParseException -> shortMessage
	is UnknownHostException,
	is SocketTimeoutException,
	-> resources.getString(R.string.network_error)

	is WrongPasswordException -> resources.getString(R.string.wrong_password)
	is NotFoundException -> resources.getString(R.string.not_found_404)

	is HttpStatusException -> when (statusCode) {
		in 500..599 -> resources.getString(R.string.server_error, statusCode)
		else -> localizedMessage
	}

	is IOException -> getDisplayMessage(message, resources) ?: localizedMessage
	else -> localizedMessage
}.ifNullOrEmpty {
	resources.getString(R.string.error_occurred)
}

private fun getDisplayMessage(msg: String?, resources: Resources): String? = when {
	msg.isNullOrEmpty() -> null
	msg.contains(MSG_NO_SPACE_LEFT) -> resources.getString(R.string.error_no_space_left)
	else -> null
}

fun Throwable.isReportable(): Boolean {
	return this is Error || this.javaClass in reportableExceptions
}

fun Throwable.report() {
	val exception = CaughtException(this, "${javaClass.simpleName}($message)")
	exception.sendWithAcra()
}

private val reportableExceptions = arraySetOf<Class<*>>(
	ParseException::class.java,
	JSONException::class.java,
	RuntimeException::class.java,
	IllegalStateException::class.java,
	IllegalArgumentException::class.java,
	ConcurrentModificationException::class.java,
	UnsupportedOperationException::class.java,
)

fun Throwable.isWebViewUnavailable(): Boolean {
	return (this is AndroidRuntimeException && message?.contains("WebView") == true) ||
		cause?.isWebViewUnavailable() == true
}

@Suppress("FunctionName")
fun NoSpaceLeftException() = IOException(MSG_NO_SPACE_LEFT)
