package org.koitharu.kotatsu.utils.ext

import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements

fun Response.parseHtml(): Document {
	try {
		(body?.byteStream() ?: throw NullPointerException("Response body is null")).use { stream ->
			val charset = body!!.contentType()?.charset()?.name()
			return Jsoup.parse(
				stream,
				charset,
				request.url.toString()
			)
		}
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

fun Response.parseJsonArray(): JSONArray {
	try {
		val string = body?.string() ?: throw NullPointerException("Response body is null")
		return JSONArray(string)
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

fun String.inContextOf(node: Node): String {
	return if (this.isEmpty()) {
		""
	} else {
		StringUtil.resolve(node.baseUri(), this)
	}
}

fun String.toRelativeUrl(domain: String): String {
	if (isEmpty() || startsWith("/")) {
		return this
	}
	return replace(Regex("^[^/]{2,6}://${Regex.escape(domain)}+/", RegexOption.IGNORE_CASE), "/")
}

fun Element.relUrl(attributeKey: String): String {
	val attr = attr(attributeKey).trim()
	if (attr.isEmpty()) {
		return ""
	}
	if (attr.startsWith("/")) {
		return attr
	}
	val baseUrl = REGEX_URL_BASE.find(baseUri())?.value ?: return attr
	return attr.removePrefix(baseUrl.dropLast(1))
}

private val REGEX_URL_BASE = Regex("^[^/]{2,6}://[^/]+/", RegexOption.IGNORE_CASE)

fun Element.css(property: String): String? {
	val regex = Regex("${Regex.escape(property)}\\s*:\\s*[^;]+")
	val css = attr("style").find(regex) ?: return null
	return css.substringAfter(':').removeSuffix(';').trim()
}