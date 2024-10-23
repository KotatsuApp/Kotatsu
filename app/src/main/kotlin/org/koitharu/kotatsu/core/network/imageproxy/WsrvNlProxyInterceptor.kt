package org.koitharu.kotatsu.core.network.imageproxy

import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.isOriginal
import okhttp3.HttpUrl
import okhttp3.Request

class WsrvNlProxyInterceptor : BaseImageProxyInterceptor() {

	override suspend fun onInterceptImageRequest(request: ImageRequest, url: HttpUrl): ImageRequest {
		val newUrl = HttpUrl.Builder()
			.scheme("https")
			.host("wsrv.nl")
			.addQueryParameter("url", url.toString())
			.addQueryParameter("we", null)
		val size = request.sizeResolver.size()
		if (!size.isOriginal) {
			newUrl.addQueryParameter("crop", "cover")
			(size.height as? Dimension.Pixels)?.let { newUrl.addQueryParameter("h", it.toString()) }
			(size.width as? Dimension.Pixels)?.let { newUrl.addQueryParameter("w", it.toString()) }
		}

		return request.newBuilder()
			.data(newUrl.build())
			.build()
	}

	override suspend fun onInterceptPageRequest(request: Request): Request {
		val sourceUrl = request.url
		val targetUrl = HttpUrl.Builder()
			.scheme("https")
			.host("wsrv.nl")
			.addQueryParameter("url", sourceUrl.toString())
			.addQueryParameter("we", null)
		return request.newBuilder()
			.url(targetUrl.build())
			.build()
	}
}
