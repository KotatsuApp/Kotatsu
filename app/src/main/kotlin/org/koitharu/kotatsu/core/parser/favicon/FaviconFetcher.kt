package org.koitharu.kotatsu.core.parser.favicon

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.network.HttpException
import coil.request.Options
import coil.size.Size
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.Closeable
import okio.buffer
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.EmptyMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.ParserMangaRepository
import org.koitharu.kotatsu.core.parser.external.ExternalMangaRepository
import org.koitharu.kotatsu.core.util.ext.requireBody
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.local.data.CacheDir
import org.koitharu.kotatsu.local.data.util.withExtraCloseable
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.await
import java.net.HttpURLConnection
import kotlin.coroutines.coroutineContext

private const val FALLBACK_SIZE = 9999 // largest icon

class FaviconFetcher(
	private val okHttpClient: OkHttpClient,
	private val diskCache: Lazy<DiskCache?>,
	private val mangaSource: MangaSource,
	private val options: Options,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) : Fetcher {

	private val diskCacheKey
		get() = options.diskCacheKey ?: "${mangaSource.name}x${options.size.toCacheKey()}"

	private val fileSystem
		get() = checkNotNull(diskCache.value).fileSystem

	override suspend fun fetch(): FetchResult {
		getCached(options)?.let { return it }
		return when (val repo = mangaRepositoryFactory.create(mangaSource)) {
			is ParserMangaRepository -> fetchParserFavicon(repo)
			is ExternalMangaRepository -> fetchPluginIcon(repo)
			is EmptyMangaRepository -> DrawableResult(
				drawable = ColorDrawable(Color.WHITE),
				isSampled = false,
				dataSource = DataSource.MEMORY,
			)

			else -> throw IllegalArgumentException("")
		}
	}

	private suspend fun fetchParserFavicon(repo: ParserMangaRepository): FetchResult {
		val sizePx = maxOf(
			options.size.width.pxOrElse { FALLBACK_SIZE },
			options.size.height.pxOrElse { FALLBACK_SIZE },
		)
		var favicons = repo.getFavicons()
		var lastError: Exception? = null
		while (favicons.isNotEmpty()) {
			coroutineContext.ensureActive()
			val icon = favicons.find(sizePx) ?: throwNSEE(lastError)
			val response = try {
				loadIcon(icon.url, mangaSource)
			} catch (e: CloudFlareProtectedException) {
				throw e
			} catch (e: HttpException) {
				lastError = e
				favicons -= icon
				continue
			}
			val responseBody = response.requireBody()
			val source = writeToDiskCache(responseBody)?.toImageSource()?.also {
				response.closeQuietly()
			} ?: responseBody.toImageSource(response)
			return SourceResult(
				source = source,
				mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(icon.type),
				dataSource = response.toDataSource(),
			)
		}
		throwNSEE(lastError)
	}

	private suspend fun loadIcon(url: String, source: MangaSource): Response {
		val request = Request.Builder()
			.url(url)
			.get()
			.tag(MangaSource::class.java, source)
		@Suppress("UNCHECKED_CAST")
		options.tags.asMap().forEach { request.tag(it.key as Class<Any>, it.value) }
		val response = okHttpClient.newCall(request.build()).await()
		if (!response.isSuccessful && response.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
			response.closeQuietly()
			throw HttpException(response)
		}
		return response
	}

	private suspend fun fetchPluginIcon(repository: ExternalMangaRepository): FetchResult {
		val source = repository.source
		val pm = options.context.packageManager
		val icon = runInterruptible(Dispatchers.IO) {
			val provider = pm.resolveContentProvider(source.authority, 0)
			provider?.loadIcon(pm) ?: pm.getApplicationIcon(source.packageName)
		}
		return DrawableResult(
			drawable = icon.nonAdaptive(),
			isSampled = false,
			dataSource = DataSource.DISK,
		)
	}

	private fun getCached(options: Options): SourceResult? {
		if (!options.diskCachePolicy.readEnabled) {
			return null
		}
		val snapshot = diskCache.value?.openSnapshot(diskCacheKey) ?: return null
		return SourceResult(
			source = snapshot.toImageSource(),
			mimeType = null,
			dataSource = DataSource.DISK,
		)
	}

	private suspend fun writeToDiskCache(body: ResponseBody): DiskCache.Snapshot? {
		if (!options.diskCachePolicy.writeEnabled || body.contentLength() == 0L) {
			return null
		}
		val editor = diskCache.value?.openEditor(diskCacheKey) ?: return null
		try {
			fileSystem.write(editor.data) {
				writeAllCancellable(body.source())
			}
			return editor.commitAndOpenSnapshot()
		} catch (e: Throwable) {
			try {
				editor.abort()
			} catch (abortingError: Throwable) {
				e.addSuppressed(abortingError)
			}
			body.closeQuietly()
			throw e
		} finally {
			body.closeQuietly()
		}
	}

	private fun DiskCache.Snapshot.toImageSource(): ImageSource {
		return ImageSource(data, fileSystem, diskCacheKey, this)
	}

	private fun ResponseBody.toImageSource(response: Closeable): ImageSource {
		return ImageSource(
			source().withExtraCloseable(response).buffer(),
			options.context,
			FaviconMetadata(mangaSource),
		)
	}

	private fun Response.toDataSource(): DataSource {
		return if (networkResponse != null) DataSource.NETWORK else DataSource.DISK
	}

	private fun Size.toCacheKey() = buildString {
		append(width.toString())
		append('x')
		append(height.toString())
	}

	private fun throwNSEE(lastError: Exception?): Nothing {
		if (lastError != null) {
			throw lastError
		} else {
			throw NoSuchElementException("No favicons found")
		}
	}

	private fun Drawable.nonAdaptive() =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
			LayerDrawable(arrayOf(background, foreground))
		} else {
			this
		}

	class Factory(
		context: Context,
		okHttpClientLazy: Lazy<OkHttpClient>,
		private val mangaRepositoryFactory: MangaRepository.Factory,
	) : Fetcher.Factory<Uri> {

		private val okHttpClient by okHttpClientLazy
		private val diskCache = lazy {
			val rootDir = context.externalCacheDir ?: context.cacheDir
			DiskCache.Builder()
				.directory(rootDir.resolve(CacheDir.FAVICONS.dir))
				.build()
		}

		override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
			return if (data.scheme == URI_SCHEME_FAVICON) {
				val mangaSource = MangaSource(data.schemeSpecificPart)
				FaviconFetcher(okHttpClient, diskCache, mangaSource, options, mangaRepositoryFactory)
			} else {
				null
			}
		}
	}

	class FaviconMetadata(val source: MangaSource) : ImageSource.Metadata()
}
