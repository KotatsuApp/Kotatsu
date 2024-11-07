package org.koitharu.kotatsu.core.network.imageproxy

import android.util.Log
import androidx.collection.ArraySet
import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.HttpStatusException
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.exceptions.CloudFlareBlockedException
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.core.util.ext.isHttpOrHttps
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.net.HttpURLConnection
import java.util.Collections

abstract class BaseImageProxyInterceptor : ImageProxyInterceptor {

	private val blacklist = Collections.synchronizedSet(ArraySet<String>())

	final override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		val request = chain.request
		val url: HttpUrl? = when (val data = request.data) {
			is HttpUrl -> data
			is String -> data.toHttpUrlOrNull()
			else -> null
		}
		if (url == null || !url.isHttpOrHttps || url.host in blacklist) {
			return chain.proceed()
		}
		val newRequest = onInterceptImageRequest(request, url)
		return when (val result = chain.withRequest(newRequest).proceed()) {
			is SuccessResult -> result
			is ErrorResult -> {
				logDebug(result.throwable, newRequest.data)
				chain.proceed().also {
					if (it is SuccessResult && result.throwable.isBlockedByServer()) {
						blacklist.add(url.host)
					}
				}
			}
		}
	}

	final override suspend fun interceptPageRequest(request: Request, okHttp: OkHttpClient): Response {
		val newRequest = onInterceptPageRequest(request)
		return runCatchingCancellable {
			okHttp.doCall(newRequest)
		}.recover { error ->
			logDebug(error, newRequest.url)
			okHttp.doCall(request).also {
				if (error.isBlockedByServer()) {
					blacklist.add(request.url.host)
				}
			}
		}.getOrThrow()
	}

	protected abstract suspend fun onInterceptImageRequest(request: ImageRequest, url: HttpUrl): ImageRequest

	protected abstract suspend fun onInterceptPageRequest(request: Request): Request

	private suspend fun OkHttpClient.doCall(request: Request): Response {
		return newCall(request).await().ensureSuccess()
	}

	private fun logDebug(e: Throwable, url: Any) {
		if (BuildConfig.DEBUG) {
			Log.w("ImageProxy", "${e.message}: $url", e)
		}
	}

	private fun Throwable.isBlockedByServer(): Boolean {
		return this is CloudFlareBlockedException
			|| (this is HttpException && response.code == HttpURLConnection.HTTP_FORBIDDEN)
			|| (this is HttpStatusException && statusCode == HttpURLConnection.HTTP_FORBIDDEN)
	}
}
