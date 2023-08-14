package org.koitharu.kotatsu.core.exceptions

import okhttp3.Headers
import okio.IOException
import org.koitharu.kotatsu.parsers.model.MangaSource

class CloudFlareProtectedException(
	val url: String,
	val source: MangaSource?,
	@Transient val headers: Headers,
) : IOException("Protected by CloudFlare")
