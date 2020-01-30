package org.koitharu.kotatsu.utils.ext

import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun Response.parseHtml(): Document {
	val stream = body?.byteStream() ?: throw NullPointerException("Response body is null")
	val charset = body!!.contentType()?.charset()?.name()
	val doc = Jsoup.parse(
		stream,
		charset,
		this.request.url.toString()
	)
	closeQuietly()
	return doc
}