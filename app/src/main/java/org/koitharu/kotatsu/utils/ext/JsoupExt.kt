package org.koitharu.kotatsu.utils.ext

import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

fun Response.parseHtml(): Document {
	try {
		val stream = body?.byteStream() ?: throw NullPointerException("Response body is null")
		val charset = body!!.contentType()?.charset()?.name()
		return Jsoup.parse(
			stream,
			charset,
			request.url.toString()
		)
	} finally {
		closeQuietly()
	}
}

fun Response.parseJson(): JSONObject {
	try {
		val string = body?.string() ?: throw NullPointerException("Response body is null")
		return JSONObject(string)
	} finally {
		closeQuietly()
	}
}

fun Element.firstChild(): Element? = children().first()