package org.koitharu.kotatsu.core.util.ext

import android.content.ActivityNotFoundException
import android.content.res.Resources
import android.util.AndroidRuntimeException
import androidx.annotation.DrawableRes
import androidx.collection.arraySetOf
import coil.network.HttpException
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
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_BOTH_LOCALE_GENRES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_BOTH_STATES_GENRES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_MULTIPLE_GENRES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_MULTIPLE_STATES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.SEARCH_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.ContentUnavailableException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.exception.ParseException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val MSG_NO_SPACE_LEFT = "No space left on device"
private const val IMAGE_FORMAT_NOT_SUPPORTED = "Image format not supported"

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

	is HttpException -> getHttpDisplayMessage(response.code, resources)
	is HttpStatusException -> getHttpDisplayMessage(statusCode, resources)

	else -> getDisplayMessage(message, resources) ?: localizedMessage
}.ifNullOrEmpty {
	resources.getString(R.string.error_occurred)
}

@DrawableRes
fun Throwable.getDisplayIcon() = when (this) {
	is AuthRequiredException -> R.drawable.ic_auth_key_large
	is CloudFlareProtectedException -> R.drawable.ic_bot_large
	is UnknownHostException,
	is SocketTimeoutException,
	-> R.drawable.ic_plug_large

	else -> R.drawable.ic_error_large
}

private fun getHttpDisplayMessage(statusCode: Int, resources: Resources): String? = when (statusCode) {
	404 -> resources.getString(R.string.not_found_404)
	in 500..599 -> resources.getString(R.string.server_error, statusCode)
	else -> null
}

private fun getDisplayMessage(msg: String?, resources: Resources): String? = when {
	msg.isNullOrEmpty() -> null
	msg.contains(MSG_NO_SPACE_LEFT) -> resources.getString(R.string.error_no_space_left)
	msg.contains(IMAGE_FORMAT_NOT_SUPPORTED) -> resources.getString(R.string.error_corrupted_file)
	msg == FILTER_MULTIPLE_GENRES_NOT_SUPPORTED -> resources.getString(R.string.error_multiple_genres_not_supported)
	msg == FILTER_MULTIPLE_STATES_NOT_SUPPORTED -> resources.getString(R.string.error_multiple_states_not_supported)
	msg == SEARCH_NOT_SUPPORTED -> resources.getString(R.string.error_search_not_supported)
	msg == FILTER_BOTH_LOCALE_GENRES_NOT_SUPPORTED -> resources.getString(R.string.error_filter_locale_genre_not_supported)
	msg == FILTER_BOTH_STATES_GENRES_NOT_SUPPORTED -> resources.getString(R.string.error_filter_states_genre_not_supported)
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
