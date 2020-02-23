package org.koitharu.kotatsu.utils.ext

import okhttp3.Response

val Response.mimeType: String?
	get() = body?.contentType()?.run { "$type/$subtype" }

val Response.contentDisposition: String?
	get() = header("Content-Disposition")