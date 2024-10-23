package org.koitharu.kotatsu.core.network.imageproxy

import coil3.intercept.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface ImageProxyInterceptor : Interceptor {

	suspend fun interceptPageRequest(request: Request, okHttp: OkHttpClient): Response
}
