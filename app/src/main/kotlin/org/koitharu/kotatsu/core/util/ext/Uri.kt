package org.koitharu.kotatsu.core.util.ext

import android.net.Uri
import androidx.core.net.toFile
import okio.Source
import okio.source
import okio.use
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.local.data.util.withExtraCloseable
import java.io.File
import java.util.zip.ZipFile

const val URI_SCHEME_FILE = "file"
const val URI_SCHEME_ZIP = "file+zip"

@Blocking
fun Uri.exists(): Boolean = when (scheme) {
	URI_SCHEME_FILE -> toFile().exists()
	URI_SCHEME_ZIP -> {
		val file = File(requireNotNull(schemeSpecificPart))
		file.exists() && ZipFile(file).use { it.getEntry(fragment) != null }
	}

	else -> unsupportedUri(this)
}

@Blocking
fun Uri.isTargetNotEmpty(): Boolean = when (scheme) {
	URI_SCHEME_FILE -> toFile().isNotEmpty()
	URI_SCHEME_ZIP -> {
		val file = File(requireNotNull(schemeSpecificPart))
		file.exists() && ZipFile(file).use { (it.getEntry(fragment)?.size ?: 0L) != 0L }
	}

	else -> unsupportedUri(this)
}

@Blocking
fun Uri.source(): Source = when (scheme) {
	URI_SCHEME_FILE -> toFile().source()
	URI_SCHEME_ZIP -> {
		val zip = ZipFile(schemeSpecificPart)
		val entry = zip.getEntry(fragment)
		zip.getInputStream(entry).source().withExtraCloseable(zip)
	}

	else -> unsupportedUri(this)
}

fun File.toZipUri(entryName: String): Uri = Uri.parse("$URI_SCHEME_ZIP://$absolutePath#$entryName")

fun String.toUriOrNull() = if (isEmpty()) null else Uri.parse(this)

private fun unsupportedUri(uri: Uri): Nothing {
	throw IllegalArgumentException("Bad uri $uri: only schemes $URI_SCHEME_FILE and $URI_SCHEME_ZIP are supported")
}
