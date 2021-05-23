package org.koitharu.kotatsu.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.StandardCharsets

class CurlLoggingInterceptor(
	private val extraCurlOptions: String? = null,
) : Interceptor {

	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request: Request = chain.request()
		var compressed = false
		val curlCmd = StringBuilder("curl")
		if (extraCurlOptions != null) {
			curlCmd.append(" ").append(extraCurlOptions)
		}
		curlCmd.append(" -X ").append(request.method)
		val headers = request.headers
		var i = 0
		val count = headers.size
		while (i < count) {
			val name = headers.name(i)
			val value = headers.value(i)
			if ("Accept-Encoding".equals(name, ignoreCase = true) && "gzip".equals(value,
					ignoreCase = true)
			) {
				compressed = true
			}
			curlCmd.append(" -H " + "\"").append(name).append(": ").append(value).append("\"")
			i++
		}
		val requestBody = request.body
		if (requestBody != null) {
			val buffer = Buffer()
			requestBody.writeTo(buffer)
			val contentType = requestBody.contentType()
			val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
			curlCmd.append(" --data $'")
				.append(buffer.readString(charset).replace("\n", "\\n"))
				.append("'")
		}
		curlCmd.append(if (compressed) " --compressed " else " ").append(request.url)
		Log.d(TAG, "╭--- cURL (" + request.url + ")")
		Log.d(TAG, curlCmd.toString())
		Log.d(TAG, "╰--- (copy and paste the above line to a terminal)")
		return chain.proceed(request)
	}

	private companion object {

		const val TAG = "CURL"
	}
}