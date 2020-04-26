package org.koitharu.kotatsu.utils.ext

import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

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

inline fun Elements.findOwnText(predicate: (String) -> Boolean): String? {
	for (x in this) {
		val ownText = x.ownText()
		if (predicate(ownText)) {
			return ownText
		}
	}
	return null
}

inline fun Elements.findText(predicate: (String) -> Boolean): String? {
	for (x in this) {
		val text = x.text()
		if (predicate(text)) {
			return text
		}
	}
	return null
}