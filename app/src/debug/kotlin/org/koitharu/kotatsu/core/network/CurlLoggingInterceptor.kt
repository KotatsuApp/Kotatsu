package org.koitharu.kotatsu.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import org.koitharu.kotatsu.core.network.CommonHeaders.ACCEPT_ENCODING

class CurlLoggingInterceptor(
	private val curlOptions: String? = null
) : Interceptor {

	private val escapeRegex = Regex("([\\[\\]\"])")

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		var isCompressed = false

		val curlCmd = StringBuilder()
		curlCmd.append("curl")
		if (curlOptions != null) {
			curlCmd.append(' ').append(curlOptions)
		}
		curlCmd.append(" -X ").append(request.method)

		for ((name, value) in request.headers) {
			if (name.equals(ACCEPT_ENCODING, ignoreCase = true) && value.equals("gzip", ignoreCase = true)) {
				isCompressed = true
			}
			curlCmd.append(" -H \"").append(name).append(": ").append(value.escape()).append('\"')
		}

		val body = request.body
		if (body != null) {
			val buffer = Buffer()
			body.writeTo(buffer)
			val charset = body.contentType()?.charset() ?: Charsets.UTF_8
			curlCmd.append(" --data-raw '")
				.append(buffer.readString(charset).replace("\n", "\\n"))
				.append("'")
		}
		if (isCompressed) {
			curlCmd.append(" --compressed")
		}
		curlCmd.append(" \"").append(request.url.toString().escape()).append('"')

		log("---cURL (" + request.url + ")")
		log(curlCmd.toString())

		return chain.proceed(request)
	}

	private fun String.escape() = replace(escapeRegex) { match ->
		"\\" + match.value
	}
		// .replace("\"", "\\\"")
		// .replace("[", "\\[")
		// .replace("]", "\\]")

	private fun log(msg: String) {
		Log.d("CURL", msg)
	}
}
