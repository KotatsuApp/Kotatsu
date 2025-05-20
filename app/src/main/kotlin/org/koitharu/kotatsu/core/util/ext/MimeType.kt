package org.koitharu.kotatsu.core.util.ext

import okhttp3.MediaType

private const val TYPE_IMAGE = "image"
private val REGEX_MIME = Regex("^\\w+/([-+.\\w]+|\\*)$", RegexOption.IGNORE_CASE)

@JvmInline
value class MimeType(private val value: String) {

	val type: String?
		get() = value.substringBefore('/', "").takeIfSpecified()

	val subtype: String?
		get() = value.substringAfterLast('/', "").takeIfSpecified()

	private fun String.takeIfSpecified(): String? = takeUnless {
		it.isEmpty() || it == "*"
	}

	override fun toString(): String = value
}

fun MediaType.toMimeType(): MimeType = MimeType("$type/$subtype")

fun String.toMimeTypeOrNull(): MimeType? = if (REGEX_MIME.matches(this)) {
	MimeType(lowercase())
} else {
	null
}

val MimeType.isImage: Boolean
	get() = type == TYPE_IMAGE
