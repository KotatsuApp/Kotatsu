package org.koitharu.kotatsu.core.util.ext

import android.content.ActivityNotFoundException
import android.content.res.Resources
import androidx.annotation.DrawableRes
import coil3.network.HttpException
import com.davemorrissey.labs.subscaleview.decoder.ImageDecodeException
import okhttp3.Response
import okio.FileNotFoundException
import okio.IOException
import okio.ProtocolException
import org.acra.ktx.sendSilentlyWithAcra
import org.acra.ktx.sendWithAcra
import org.jsoup.HttpStatusException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.BadBackupFormatException
import org.koitharu.kotatsu.core.exceptions.CaughtException
import org.koitharu.kotatsu.core.exceptions.CloudFlareBlockedException
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.exceptions.EmptyHistoryException
import org.koitharu.kotatsu.core.exceptions.IncompatiblePluginException
import org.koitharu.kotatsu.core.exceptions.NoDataReceivedException
import org.koitharu.kotatsu.core.exceptions.ProxyConfigException
import org.koitharu.kotatsu.core.exceptions.SyncApiException
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.core.exceptions.WrapperIOException
import org.koitharu.kotatsu.core.exceptions.WrongPasswordException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.io.NullOutputStream
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_BOTH_LOCALE_GENRES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_BOTH_STATES_GENRES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_MULTIPLE_GENRES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.FILTER_MULTIPLE_STATES_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.ErrorMessages.SEARCH_NOT_SUPPORTED
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.ContentUnavailableException
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.exception.TooManyRequestExceptions
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import org.koitharu.kotatsu.scrobbling.common.domain.ScrobblerAuthRequiredException
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

private const val MSG_NO_SPACE_LEFT = "No space left on device"
private const val MSG_CONNECTION_RESET = "Connection reset"
private const val IMAGE_FORMAT_NOT_SUPPORTED = "Image format not supported"

fun Throwable.getDisplayMessage(resources: Resources): String = getDisplayMessageOrNull(resources)
	?: resources.getString(R.string.error_occurred)

private fun Throwable.getDisplayMessageOrNull(resources: Resources): String? = when (this) {
	is CaughtException -> cause.getDisplayMessageOrNull(resources)
	is WrapperIOException -> cause.getDisplayMessageOrNull(resources)
	is ScrobblerAuthRequiredException -> resources.getString(
		R.string.scrobbler_auth_required,
		resources.getString(scrobbler.titleResId),
	)

	is AuthRequiredException -> resources.getString(R.string.auth_required)
	is CloudFlareProtectedException -> resources.getString(R.string.captcha_required_message)
	is CloudFlareBlockedException -> resources.getString(R.string.blocked_by_server_message)
	is ActivityNotFoundException,
	is UnsupportedOperationException,
		-> resources.getString(R.string.operation_not_supported)

	is TooManyRequestExceptions -> {
		val delay = getRetryDelay()
		val formattedTime = if (delay > 0L && delay < Long.MAX_VALUE) {
			resources.formatDurationShort(delay)
		} else {
			null
		}
		if (formattedTime != null) {
			resources.getString(R.string.too_many_requests_message_retry, formattedTime)
		} else {
			resources.getString(R.string.too_many_requests_message)
		}
	}

	is UnsupportedFileException -> resources.getString(R.string.text_file_not_supported)
	is BadBackupFormatException -> resources.getString(R.string.unsupported_backup_message)
	is FileNotFoundException -> resources.getString(R.string.file_not_found)
	is AccessDeniedException -> resources.getString(R.string.no_access_to_file)
	is EmptyHistoryException -> resources.getString(R.string.history_is_empty)
	is ProxyConfigException -> resources.getString(R.string.invalid_proxy_configuration)
	is SyncApiException,
	is ContentUnavailableException -> message

	is ParseException -> shortMessage
	is ConnectException,
	is UnknownHostException,
	is NoRouteToHostException,
	is SocketTimeoutException -> resources.getString(R.string.network_error)

	is ImageDecodeException -> {
		val type = format?.substringBefore('/')
		val formatString = format.ifNullOrEmpty { resources.getString(R.string.unknown).lowercase(Locale.getDefault()) }
		if (type.isNullOrEmpty() || type == "image") {
			resources.getString(R.string.error_image_format, formatString)
		} else {
			resources.getString(R.string.error_not_image, formatString)
		}
	}

	is NoDataReceivedException -> resources.getString(R.string.error_no_data_received)
	is IncompatiblePluginException -> {
		cause?.getDisplayMessageOrNull(resources)?.let {
			resources.getString(R.string.plugin_incompatible_with_cause, it)
		} ?: resources.getString(R.string.plugin_incompatible)
	}

	is WrongPasswordException -> resources.getString(R.string.wrong_password)
	is NotFoundException -> resources.getString(R.string.not_found_404)
	is UnsupportedSourceException -> resources.getString(R.string.unsupported_source)

	is HttpException -> getHttpDisplayMessage(response.code, resources)
	is HttpStatusException -> getHttpDisplayMessage(statusCode, resources)

	else -> mapDisplayMessage(message, resources) ?: message
}.takeUnless { it.isNullOrBlank() }

@DrawableRes
fun Throwable.getDisplayIcon() = when (this) {
	is AuthRequiredException -> R.drawable.ic_auth_key_large
	is CloudFlareProtectedException -> R.drawable.ic_bot_large
	is UnknownHostException,
	is SocketTimeoutException,
	is ConnectException,
	is NoRouteToHostException,
	is ProtocolException -> R.drawable.ic_plug_large

	is CloudFlareBlockedException -> R.drawable.ic_denied_large

	else -> R.drawable.ic_error_large
}

fun Throwable.getCauseUrl(): String? = when (this) {
	is ParseException -> url
	is NotFoundException -> url
	is TooManyRequestExceptions -> url
	is CaughtException -> cause.getCauseUrl()
	is WrapperIOException -> cause.getCauseUrl()
	is NoDataReceivedException -> url
	is CloudFlareBlockedException -> url
	is CloudFlareProtectedException -> url
	is HttpStatusException -> url
	is HttpException -> (response.delegate as? Response)?.request?.url?.toString()
	else -> null
}

private fun getHttpDisplayMessage(statusCode: Int, resources: Resources): String? = when (statusCode) {
	404 -> resources.getString(R.string.not_found_404)
	403 -> resources.getString(R.string.access_denied_403)
	in 500..599 -> resources.getString(R.string.server_error, statusCode)
	else -> null
}

private fun mapDisplayMessage(msg: String?, resources: Resources): String? = when {
	msg.isNullOrEmpty() -> null
	msg.contains(MSG_NO_SPACE_LEFT) -> resources.getString(R.string.error_no_space_left)
	msg.contains(IMAGE_FORMAT_NOT_SUPPORTED) -> resources.getString(R.string.error_corrupted_file)
	msg == MSG_CONNECTION_RESET -> resources.getString(R.string.error_connection_reset)
	msg == FILTER_MULTIPLE_GENRES_NOT_SUPPORTED -> resources.getString(R.string.error_multiple_genres_not_supported)
	msg == FILTER_MULTIPLE_STATES_NOT_SUPPORTED -> resources.getString(R.string.error_multiple_states_not_supported)
	msg == SEARCH_NOT_SUPPORTED -> resources.getString(R.string.error_search_not_supported)
	msg == FILTER_BOTH_LOCALE_GENRES_NOT_SUPPORTED -> resources.getString(R.string.error_filter_locale_genre_not_supported)
	msg == FILTER_BOTH_STATES_GENRES_NOT_SUPPORTED -> resources.getString(R.string.error_filter_states_genre_not_supported)
	else -> null
}

fun Throwable.isReportable(): Boolean {
	if (this is Error) {
		return true
	}
	if (this is CaughtException) {
		return cause.isReportable()
	}
	if (this is WrapperIOException) {
		return cause.isReportable()
	}
	if (ExceptionResolver.canResolve(this)) {
		return false
	}
	if (this is ParseException
		|| this.isNetworkError()
		|| this is CloudFlareBlockedException
		|| this is CloudFlareProtectedException
		|| this is BadBackupFormatException
		|| this is WrongPasswordException
		|| this is TooManyRequestExceptions
		|| this is HttpStatusException
		|| this is SocketException
	) {
		return false
	}
	return true
}

fun Throwable.isNetworkError(): Boolean {
	return this is UnknownHostException || this is SocketTimeoutException
}

fun Throwable.report(silent: Boolean = false) {
	val exception = CaughtException(this)
	if (silent) {
		exception.sendSilentlyWithAcra()
	} else {
		exception.sendWithAcra()
	}
}

fun Throwable.isWebViewUnavailable(): Boolean {
	val trace = stackTraceToString()
	return trace.contains("android.webkit.WebView.<init>")
}

@Suppress("FunctionName")
fun NoSpaceLeftException() = IOException(MSG_NO_SPACE_LEFT)

fun Throwable.isSerializable() = runCatching {
	val oos = ObjectOutputStream(NullOutputStream())
	oos.writeObject(this)
	oos.flush()
}.isSuccess
