package org.koitharu.kotatsu.details.ui.pager.pages

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.Options
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.network.imageproxy.ImageProxyInterceptor
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.util.ext.fetch
import org.koitharu.kotatsu.core.util.ext.isNetworkUri
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.mimeType
import org.koitharu.kotatsu.parsers.util.requireBody
import org.koitharu.kotatsu.reader.domain.PageLoader
import javax.inject.Inject

class MangaPageFetcher(
	private val okHttpClient: OkHttpClient,
	private val pagesCache: PagesCache,
	private val options: Options,
	private val page: MangaPage,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val imageProxyInterceptor: ImageProxyInterceptor,
	private val imageLoader: ImageLoader,
) : Fetcher {

	override suspend fun fetch(): FetchResult? {
		val repo = mangaRepositoryFactory.create(page.source)
		val pageUrl = repo.getPageUrl(page)
		if (options.diskCachePolicy.readEnabled) {
			pagesCache.get(pageUrl)?.let { file ->
				return SourceFetchResult(
					source = ImageSource(file.toOkioPath(), options.fileSystem),
					mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
					dataSource = DataSource.DISK,
				)
			}
		}
		return loadPage(pageUrl)
	}

	private suspend fun loadPage(pageUrl: String): FetchResult? = if (pageUrl.toUri().isNetworkUri()) {
		fetchPage(pageUrl)
	} else {
		imageLoader.fetch(pageUrl, options)
	}

	private suspend fun fetchPage(pageUrl: String): FetchResult {
		val request = PageLoader.createPageRequest(pageUrl, page.source)
		return imageProxyInterceptor.interceptPageRequest(request, okHttpClient).use { response ->
			if (!response.isSuccessful) {
				throw HttpException(response.toNetworkResponse())
			}
			val mimeType = response.mimeType
			val file = response.requireBody().use {
				pagesCache.put(pageUrl, it.source(), mimeType)
			}
			SourceFetchResult(
				source = ImageSource(file.toOkioPath(), FileSystem.SYSTEM),
				mimeType = mimeType,
				dataSource = DataSource.NETWORK,
			)
		}
	}

	private fun Response.toNetworkResponse(): NetworkResponse {
		return NetworkResponse(
			code = code,
			requestMillis = sentRequestAtMillis,
			responseMillis = receivedResponseAtMillis,
			headers = headers.toNetworkHeaders(),
			body = body?.source()?.let(::NetworkResponseBody),
			delegate = this,
		)
	}

	private fun Headers.toNetworkHeaders(): NetworkHeaders {
		val headers = NetworkHeaders.Builder()
		for ((key, values) in this) {
			headers.add(key, values)
		}
		return headers.build()
	}

	class Factory @Inject constructor(
		@MangaHttpClient private val okHttpClient: OkHttpClient,
		private val pagesCache: PagesCache,
		private val mangaRepositoryFactory: MangaRepository.Factory,
		private val imageProxyInterceptor: ImageProxyInterceptor,
	) : Fetcher.Factory<MangaPage> {

		override fun create(data: MangaPage, options: Options, imageLoader: ImageLoader) = MangaPageFetcher(
			okHttpClient = okHttpClient,
			pagesCache = pagesCache,
			options = options,
			page = data,
			mangaRepositoryFactory = mangaRepositoryFactory,
			imageProxyInterceptor = imageProxyInterceptor,
			imageLoader = imageLoader,
		)
	}
}
