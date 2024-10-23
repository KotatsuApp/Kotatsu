package org.koitharu.kotatsu.core.network.imageproxy

import coil3.request.ImageRequest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class ZeroMsProxyInterceptor : BaseImageProxyInterceptor() {

	override suspend fun onInterceptImageRequest(request: ImageRequest, url: HttpUrl): ImageRequest {
		if (url.host == "x.0ms.dev" || url.host == "0ms.dev") {
			return request
		}
		val newUrl = ("https://x.0ms.dev/q70/$url").toHttpUrl()
		return request.newBuilder()
			.data(newUrl)
			.build()
	}

	override suspend fun onInterceptPageRequest(request: Request): Request {
		val newUrl = ("https://x.0ms.dev/q70/${request.url}").toHttpUrl()
		return request.newBuilder()
			.url(newUrl)
			.build()
	}
}
