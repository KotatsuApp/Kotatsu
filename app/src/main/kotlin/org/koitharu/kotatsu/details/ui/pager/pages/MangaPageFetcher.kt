package org.koitharu.kotatsu.details.ui.pager.pages

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
import org.koitharu.kotatsu.core.util.MimeTypes
import org.koitharu.kotatsu.core.util.ext.fetch
import org.koitharu.kotatsu.core.util.ext.isNetworkUri
import org.koitharu.kotatsu.core.util.ext.toMimeTypeOrNull
import org.koitharu.kotatsu.local.data.LocalStorageCache
import org.koitharu.kotatsu.local.data.PageCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.mimeType
import org.koitharu.kotatsu.parsers.util.requireBody
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.domain.PageLoader
import javax.inject.Inject

class MangaPageFetcher(
	private val okHttpClient: OkHttpClient,
	private val pagesCache: LocalStorageCache,
	private val options: Options,
	private val page: MangaPage,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val imageProxyInterceptor: ImageProxyInterceptor,
	private val imageLoader: ImageLoader,
) : Fetcher {

	override suspend fun fetch(): FetchResult? {
		if (!page.preview.isNullOrEmpty()) {
			runCatchingCancellable {
				imageLoader.fetch(checkNotNull(page.preview), options)
			}.onSuccess {
				return it
			}
		}
		val repo = mangaRepositoryFactory.create(page.source)
		val pageUrl = repo.getPageUrl(page)
		if (options.diskCachePolicy.readEnabled) {
			pagesCache[pageUrl]?.let { file ->
				return SourceFetchResult(
					source = ImageSource(file.toOkioPath(), options.fileSystem),
					mimeType = MimeTypes.getMimeTypeFromExtension(file.name)?.toString(),
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
			val mimeType = response.mimeType?.toMimeTypeOrNull()
			val file = response.requireBody().use {
				pagesCache.set(pageUrl, it.source(), mimeType)
			}
			SourceFetchResult(
				source = ImageSource(file.toOkioPath(), FileSystem.SYSTEM),
				mimeType = mimeType?.toString(),
				dataSource = DataSource.NETWORK,
			)
		}
	}

	private fun Response.toNetworkResponse() = NetworkResponse(
		code = code,
		requestMillis = sentRequestAtMillis,
		responseMillis = receivedResponseAtMillis,
		headers = headers.toNetworkHeaders(),
		body = body?.source()?.let(::NetworkResponseBody),
		delegate = this,
	)

	private fun Headers.toNetworkHeaders(): NetworkHeaders {
		val headers = NetworkHeaders.Builder()
		for ((key, values) in this) {
			headers.add(key, values)
		}
		return headers.build()
	}

	class Factory @Inject constructor(
		@MangaHttpClient private val okHttpClient: OkHttpClient,
		@PageCache private val pagesCache: LocalStorageCache,
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
