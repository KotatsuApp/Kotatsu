package org.koitharu.kotatsu.download.domain

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.local.data.MangaZip
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.waitForNetwork
import org.koitharu.kotatsu.utils.progress.ProgressJob
import java.io.File

private const val MAX_DOWNLOAD_ATTEMPTS = 3
private const val MAX_PARALLEL_DOWNLOADS = 2
private const val DOWNLOAD_ERROR_DELAY = 500L
private const val TEMP_PAGE_FILE = "page.tmp"

class DownloadManager(
	private val coroutineScope: CoroutineScope,
	private val context: Context,
	private val imageLoader: ImageLoader,
	private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val localMangaRepository: LocalMangaRepository,
) {

	private val connectivityManager = context.applicationContext.getSystemService(
		Context.CONNECTIVITY_SERVICE
	) as ConnectivityManager
	private val coverWidth = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_width
	)
	private val coverHeight = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_height
	)
	private val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)

	fun downloadManga(
		manga: Manga,
		chaptersIds: Set<Long>?,
		startId: Int,
	): ProgressJob<DownloadState> {
		val stateFlow = MutableStateFlow<DownloadState>(
			DownloadState.Queued(startId = startId, manga = manga, cover = null)
		)
		val job = downloadMangaImpl(manga, chaptersIds, stateFlow, startId)
		return ProgressJob(job, stateFlow)
	}

	private fun downloadMangaImpl(
		manga: Manga,
		chaptersIds: Set<Long>?,
		outState: MutableStateFlow<DownloadState>,
		startId: Int,
	): Job = coroutineScope.launch(Dispatchers.Default + errorStateHandler(outState)) {
		semaphore.acquire()
		coroutineContext[WakeLockNode]?.acquire()
		outState.value = DownloadState.Preparing(startId, manga, null)
		var cover: Drawable? = null
		val destination = localMangaRepository.getOutputDir()
		checkNotNull(destination) { context.getString(R.string.cannot_find_available_storage) }
		var output: MangaZip? = null
		try {
			val repo = MangaRepository(manga.source)
			cover = runCatching {
				imageLoader.execute(
					ImageRequest.Builder(context)
						.data(manga.coverUrl)
						.referer(manga.publicUrl)
						.size(coverWidth, coverHeight)
						.scale(Scale.FILL)
						.build()
				).drawable
			}.getOrNull()
			outState.value = DownloadState.Preparing(startId, manga, cover)
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
								outState.value = DownloadState.WaitingForNetwork(startId, data, cover)
								connectivityManager.waitForNetwork()
								continue@failsafe
							}
						} while (false)

						outState.value = DownloadState.Progress(
							startId, data, cover,
							totalChapters = chapters.size,
							currentChapter = chapterIndex,
							totalPages = pages.size,
							currentPage = pageIndex,
						)
					}
				}
			}
			outState.value = DownloadState.PostProcessing(startId, data, cover)
			if (!output.compress()) {
				throw RuntimeException("Cannot create target file")
			}
			val localManga = localMangaRepository.getFromFile(output.file)
			outState.value = DownloadState.Done(startId, data, cover, localManga)
		} catch (e: CancellationException) {
			outState.value = DownloadState.Cancelled(startId, manga, cover)
			throw e
		} catch (e: Throwable) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
			outState.value = DownloadState.Error(startId, manga, cover, e)
		} finally {
			withContext(NonCancellable) {
				output?.cleanup()
				File(destination, TEMP_PAGE_FILE).deleteAwait()
			}
			coroutineContext[WakeLockNode]?.release()
			semaphore.release()
		}
	}

	private suspend fun downloadFile(url: String, referer: String, destination: File): File {
		val request = Request.Builder()
			.url(url)
			.header(CommonHeaders.REFERER, referer)
			.cacheControl(CommonHeaders.CACHE_CONTROL_DISABLED)
			.get()
			.build()
		val call = okHttp.newCall(request)
		var attempts = MAX_DOWNLOAD_ATTEMPTS
		val file = File(destination, TEMP_PAGE_FILE)
		while (true) {
			try {
				val response = call.clone().await()
				runInterruptible(Dispatchers.IO) {
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

	private fun errorStateHandler(outState: MutableStateFlow<DownloadState>) = CoroutineExceptionHandler { _, throwable ->
		val prevValue = outState.value
		outState.value = DownloadState.Error(
			startId = prevValue.startId,
			manga = prevValue.manga,
			cover = prevValue.cover,
			error = throwable,
		)
	}
}