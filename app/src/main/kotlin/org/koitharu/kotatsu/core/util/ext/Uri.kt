package org.koitharu.kotatsu.core.util.ext

import android.net.Uri
import java.io.File

const val URI_SCHEME_ZIP = "file+zip"
private const val URI_SCHEME_FILE = "file"
private const val URI_SCHEME_HTTP = "http"
private const val URI_SCHEME_HTTPS = "https"
private const val URI_SCHEME_LEGACY_CBZ = "cbz"
private const val URI_SCHEME_LEGACY_ZIP = "zip"

fun Uri.isZipUri() = scheme.let {
	it == URI_SCHEME_ZIP || it == URI_SCHEME_LEGACY_CBZ || it == URI_SCHEME_LEGACY_ZIP
}

fun Uri.isFileUri() = scheme == URI_SCHEME_FILE

fun Uri.isNetworkUri() = scheme.let {
	it == URI_SCHEME_HTTP || it == URI_SCHEME_HTTPS
}

fun File.toZipUri(entryName: String): Uri = Uri.parse("$URI_SCHEME_ZIP://$absolutePath#$entryName")

fun String.toUriOrNull() = if (isEmpty()) null else Uri.parse(this)
