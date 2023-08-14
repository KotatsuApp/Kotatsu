package org.koitharu.kotatsu.reader.ui.thumbnails

import android.content.Context
import androidx.core.net.toUri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import org.koitharu.kotatsu.core.network.ImageProxyInterceptor
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.data.util.withExtraCloseable
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.mimeType
import org.koitharu.kotatsu.reader.domain.PageLoader
import java.util.zip.ZipFile
import javax.inject.Inject

class MangaPageFetcher(
	private val context: Context,
	private val okHttpClient: OkHttpClient,
	private val pagesCache: PagesCache,
	private val options: Options,
	private val page: MangaPage,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val imageProxyInterceptor: ImageProxyInterceptor,
) : Fetcher {

	override suspend fun fetch(): FetchResult {
		val repo = mangaRepositoryFactory.create(page.source)
		val pageUrl = repo.getPageUrl(page)
		pagesCache.get(pageUrl)?.let { file ->
			return SourceResult(
				source = ImageSource(
					file = file.toOkioPath(),
					metadata = MangaPageMetadata(page),
				),
				mimeType = null,
				dataSource = DataSource.DISK,
			)
		}
		return loadPage(pageUrl)
	}

	private suspend fun loadPage(pageUrl: String): SourceResult {
		val uri = pageUrl.toUri()
		return if (CbzFilter.isUriSupported(uri)) {
			runInterruptible(Dispatchers.IO) {
				val zip = ZipFile(uri.schemeSpecificPart)
				val entry = zip.getEntry(uri.fragment)
				SourceResult(
					source = ImageSource(
						source = zip.getInputStream(entry).source().withExtraCloseable(zip).buffer(),
						context = context,
						metadata = MangaPageMetadata(page),
					),
					mimeType = null,
					dataSource = DataSource.DISK,
				)
			}
		} else {
			val request = PageLoader.createPageRequest(page, pageUrl)
			imageProxyInterceptor.interceptPageRequest(request, okHttpClient).use { response ->
				check(response.isSuccessful) {
					"Invalid response: ${response.code} ${response.message} at $pageUrl"
				}
				val body = checkNotNull(response.body) {
					"Null response"
				}
				val mimeType = response.mimeType
				val file = body.use {
					pagesCache.put(pageUrl, it.source())
				}
				SourceResult(
					source = ImageSource(
						file = file.toOkioPath(),
						metadata = MangaPageMetadata(page),
					),
					mimeType = mimeType,
					dataSource = DataSource.NETWORK,
				)
			}
		}
	}

	class Factory @Inject constructor(
		@ApplicationContext private val context: Context,
		@MangaHttpClient private val okHttpClient: OkHttpClient,
		private val pagesCache: PagesCache,
		private val mangaRepositoryFactory: MangaRepository.Factory,
		private val imageProxyInterceptor: ImageProxyInterceptor,
	) : Fetcher.Factory<MangaPage> {

		override fun create(data: MangaPage, options: Options, imageLoader: ImageLoader): Fetcher {
			return MangaPageFetcher(
				okHttpClient = okHttpClient,
				pagesCache = pagesCache,
				options = options,
				page = data,
				context = context,
				mangaRepositoryFactory = mangaRepositoryFactory,
				imageProxyInterceptor = imageProxyInterceptor,
			)
		}
	}

	class MangaPageMetadata(val page: MangaPage) : ImageSource.Metadata()
}
