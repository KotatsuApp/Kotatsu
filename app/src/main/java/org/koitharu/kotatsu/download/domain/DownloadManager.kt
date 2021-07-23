package org.koitharu.kotatsu.download.domain

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.MangaZip
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.waitForNetwork
import java.io.File

class DownloadManager(
	private val context: Context,
	private val settings: AppSettings,
	private val imageLoader: ImageLoader,
	private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val localMangaRepository: LocalMangaRepository,
) {

	private val connectivityManager = context.getSystemService(
		Context.CONNECTIVITY_SERVICE
	) as ConnectivityManager
	private val coverWidth = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_width
	)
	private val coverHeight = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_height
	)

	fun downloadManga(manga: Manga, chaptersIds: Set<Long>?, startId: Int): Flow<State> = flow {
		emit(State.Preparing(startId, manga, null))
		var cover: Drawable? = null
		val destination = settings.getStorageDir(context)
		checkNotNull(destination) { context.getString(R.string.cannot_find_available_storage) }
		var output: MangaZip? = null
		try {
			val repo = MangaRepository(manga.source)
			cover = runCatching {
				imageLoader.execute(
					ImageRequest.Builder(context)
						.data(manga.coverUrl)
						.size(coverWidth, coverHeight)
						.scale(Scale.FILL)
						.build()
				).drawable
			}.getOrNull()
			emit(State.Preparing(startId, manga, cover))
			val data = if (manga.chapters == null) repo.getDetails(manga) else manga
			output = MangaZip.findInDir(destination, data)
			output.prepare(data)
			val coverUrl = data.largeCoverUrl ?: data.coverUrl
			downloadFile(coverUrl, data.publicUrl, destination).let { file ->
				output.addCover(file, MimeTypeMap.getFileExtensionFromUrl(coverUrl))
			}
			val chapters = if (chaptersIds == null) {
				data.chapters.orEmpty()
			} else {
				data.chapters.orEmpty().filter { x -> x.id in chaptersIds }
			}
			for ((chapterIndex, chapter) in chapters.withIndex()) {
				if (chaptersIds == null || chapter.id in chaptersIds) {
					val pages = repo.getPages(chapter)
					for ((pageIndex, page) in pages.withIndex()) {
						failsafe@ do {
							try {
								val url = repo.getPageUrl(page)
								val file =
									cache[url] ?: downloadFile(url, page.referer, destination)
								output.addPage(
									chapter,
									file,
									pageIndex,
									MimeTypeMap.getFileExtensionFromUrl(url)
								)
							} catch (e: IOException) {
								emit(State.WaitingForNetwork(startId, manga, cover))
								connectivityManager.waitForNetwork()
								continue@failsafe
							}
						} while (false)

						emit(State.Progress(
							startId, manga, cover,
							totalChapters = chapters.size,
							currentChapter = chapterIndex,
							totalPages = pages.size,
							currentPage = pageIndex,
						))
					}
				}
			}
			emit(State.PostProcessing(startId, manga, cover))
			if (!output.compress()) {
				throw RuntimeException("Cannot create target file")
			}
			val localManga = localMangaRepository.getFromFile(output.file)
			emit(State.Done(startId, manga, cover, localManga))
		} catch (_: CancellationException) {
			emit(State.Cancelling(startId, manga, cover))
		} catch (e: Throwable) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
			emit(State.Error(startId, manga, cover, e))
		} finally {
			withContext(NonCancellable) {
				output?.cleanup()
				File(destination, TEMP_PAGE_FILE).deleteAwait()
			}
		}
	}.catch { e ->
		emit(State.Error(startId, manga, null, e))
	}

	private suspend fun downloadFile(url: String, referer: String, destination: File): File {
		val request = Request.Builder()
			.url(url)
			.header(CommonHeaders.REFERER, referer)
			.cacheControl(CacheUtils.CONTROL_DISABLED)
			.get()
			.build()
		val call = okHttp.newCall(request)
		var attempts = MAX_DOWNLOAD_ATTEMPTS
		val file = File(destination, TEMP_PAGE_FILE)
		while (true) {
			try {
				val response = call.clone().await()
				withContext(Dispatchers.IO) {
					file.outputStream().use { out ->
						checkNotNull(response.body).byteStream().copyTo(out)
					}
				}
				return file
			} catch (e: IOException) {
				attempts--
				if (attempts <= 0) {
					throw e
				} else {
					delay(DOWNLOAD_ERROR_DELAY)
				}
			}
		}
	}

	sealed interface State {

		val startId: Int
		val manga: Manga
		val cover: Drawable?

		data class Queued(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
		) : State

		data class Preparing(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
		) : State

		data class Progress(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
			val totalChapters: Int,
			val currentChapter: Int,
			val totalPages: Int,
			val currentPage: Int,
		): State {

			val max: Int = totalChapters * totalPages

			val progress: Int = totalPages * currentChapter + currentPage + 1

			val percent: Float = progress.toFloat() / max
		}

		data class WaitingForNetwork(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
		): State

		data class Done(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
			val localManga: Manga,
		) : State

		data class Error(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
			val error: Throwable,
		) : State

		data class Cancelling(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
		): State

		data class PostProcessing(
			override val startId: Int,
			override val manga: Manga,
			override val cover: Drawable?,
		) : State
	}

	private companion object {

		private const val MAX_DOWNLOAD_ATTEMPTS = 3
		private const val DOWNLOAD_ERROR_DELAY = 500L
		private const val TEMP_PAGE_FILE = "page.tmp"
	}
}