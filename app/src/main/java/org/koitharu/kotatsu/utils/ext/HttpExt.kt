package org.koitharu.kotatsu.utils.ext

import okhttp3.Response
import org.koitharu.kotatsu.core.network.CommonHeaders

val Response.mimeType: String?
	get() = body?.contentType()?.run { "$type/$subtype" }

val Response.contentDisposition: String?
	get() = header(CommonHeaders.CONTENT_DISPOSITION)