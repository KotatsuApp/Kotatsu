package org.koitharu.kotatsu.core.exceptions

import okio.IOException
import org.koitharu.kotatsu.parsers.model.MangaSource

abstract class CloudFlareException(
	message: String,
	val state: Int,
) : IOException(message) {

	abstract val url: String

	abstract val source: MangaSource
}
