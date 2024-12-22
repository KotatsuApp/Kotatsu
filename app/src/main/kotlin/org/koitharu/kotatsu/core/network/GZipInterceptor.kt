package org.koitharu.kotatsu.core.network

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.Response
import okio.IOException
import org.koitharu.kotatsu.core.network.CommonHeaders.CONTENT_ENCODING

class GZipInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		if (request.body is MultipartBody) {
			return chain.proceed(request)
		}
		val newRequest = request.newBuilder()
		newRequest.addHeader(CONTENT_ENCODING, "gzip")
		return try {
			chain.proceed(newRequest.build())
		} catch (e: NullPointerException) {
			throw IOException(e)
		}
	}
}
