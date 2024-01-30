package org.koitharu.kotatsu.core.network

import android.util.Log
import androidx.collection.ArraySet
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.isOriginal
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.core.util.ext.isHttpOrHttps
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageProxyInterceptor @Inject constructor(
	private val settings: AppSettings,
) : Interceptor {

	private val blacklist = Collections.synchronizedSet(ArraySet<String>())

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		val request = chain.request
		if (!settings.isImagesProxyEnabled) {
			return chain.proceed(request)
		}
		val url: HttpUrl? = when (val data = request.data) {
			is HttpUrl -> data
			is String -> data.toHttpUrlOrNull()
			else -> null
		}
		if (url == null || !url.isHttpOrHttps || url.host in blacklist) {
			return chain.proceed(request)
		}
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

		val newRequest = request.newBuilder()
			.data(newUrl.build())
			.build()
		val result = chain.proceed(newRequest)
		return if (result is SuccessResult) {
			result
		} else {
			logDebug((result as? ErrorResult)?.throwable)
			chain.proceed(request).also {
				if (it is SuccessResult) {
					blacklist.add(url.host)
				}
			}
		}
	}

	suspend fun interceptPageRequest(request: Request, okHttp: OkHttpClient): Response {
		if (!settings.isImagesProxyEnabled) {
			return okHttp.newCall(request).await()
		}
		val sourceUrl = request.url
		val targetUrl = HttpUrl.Builder()
			.scheme("https")
			.host("wsrv.nl")
			.addQueryParameter("url", sourceUrl.toString())
			.addQueryParameter("we", null)
		val newRequest = request.newBuilder()
			.url(targetUrl.build())
			.build()
		return runCatchingCancellable {
			okHttp.doCall(newRequest)
		}.recover {
			logDebug(it)
			okHttp.doCall(request).also {
				blacklist.add(sourceUrl.host)
			}
		}.getOrThrow()
	}

	private suspend fun OkHttpClient.doCall(request: Request): Response {
		return newCall(request).await().ensureSuccess()
	}

	private fun logDebug(e: Throwable?) {
		if (BuildConfig.DEBUG) {
			Log.w("ImageProxy", e.toString())
		}
	}
}
