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
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.domain.CbzMangaOutput
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.waitForNetwork
import org.koitharu.kotatsu.utils.progress.ProgressJob
import java.io.File

private const val MAX_DOWNLOAD_ATTEMPTS = 3
private const val DOWNLOAD_ERROR_DELAY = 500L
private const val SLOWDOWN_DELAY = 200L

class DownloadManager(
	private val coroutineScope: CoroutineScope,
	private val context: Context,
	private val imageLoader: ImageLoader,
	private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val localMangaRepository: LocalMangaRepository,
	private val settings: AppSettings,
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
	private val semaphore = Semaphore(settings.downloadsParallelism)

	fun downloadManga(
		manga: Manga,
		chaptersIds: LongArray?,
		startId: Int,
	): ProgressJob<DownloadState> {
		val stateFlow = MutableStateFlow<DownloadState>(
			DownloadState.Queued(startId = startId, manga = manga, cover = null)
		)
		val job = downloadMangaImpl(manga, chaptersIds?.takeUnless { it.isEmpty() }, stateFlow, startId)
		return ProgressJob(job, stateFlow)
	}

	private fun downloadMangaImpl(
		manga: Manga,
		chaptersIds: LongArray?,
		outState: MutableStateFlow<DownloadState>,
		startId: Int,
	): Job = coroutineScope.launch(Dispatchers.Default + errorStateHandler(outState)) {
		@Suppress("NAME_SHADOWING") var manga = manga
		val chaptersIdsSet = chaptersIds?.toMutableSet()
		semaphore.acquire()
		coroutineContext[WakeLockNode]?.acquire()
		outState.value = DownloadState.Preparing(startId, manga, null)
		var cover: Drawable? = null
		val destination = localMangaRepository.getOutputDir()
		checkNotNull(destination) { context.getString(R.string.cannot_find_available_storage) }
		val tempFileName = "${manga.id}_$startId.tmp"
		var output: CbzMangaOutput? = null
		try {
			if (manga.source == MangaSource.LOCAL) {
				manga = localMangaRepository.getRemoteManga(manga) ?: error("Cannot obtain remote manga instance")
			}
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
			val data = if (manga.chapters.isNullOrEmpty()) repo.getDetails(manga) else manga
			output = CbzMangaOutput.get(destination, data)
			val coverUrl = data.largeCoverUrl ?: data.coverUrl
			downloadFile(coverUrl, data.publicUrl, destination, tempFileName).let { file ->
				output.addCover(file, MimeTypeMap.getFileExtensionFromUrl(coverUrl))
			}
			val chapters = checkNotNull(
				if (chaptersIdsSet == null) {
					data.chapters
				} else {
					data.chapters?.filter { x -> chaptersIdsSet.remove(x.id) }
				}
			) { "Chapters list must not be null" }
			check(chapters.isNotEmpty()) { "Chapters list must not be empty" }
			check(chaptersIdsSet.isNullOrEmpty()) {
				"${chaptersIdsSet?.size} of ${chaptersIds?.size} requested chapters not found in manga"
			}
			for ((chapterIndex, chapter) in chapters.withIndex()) {
				val pages = repo.getPages(chapter)
				for ((pageIndex, page) in pages.withIndex()) {
					var retryCounter = 0
					failsafe@ while (true) {
						try {
							val url = repo.getPageUrl(page)
							val file = cache[url] ?: downloadFile(url, page.referer, destination, tempFileName)
							output.addPage(
								chapter = chapter,
								file = file,
								pageNumber = pageIndex,
								ext = MimeTypeMap.getFileExtensionFromUrl(url),
							)
							break@failsafe
						} catch (e: IOException) {
							if (retryCounter < MAX_DOWNLOAD_ATTEMPTS) {
								outState.value = DownloadState.WaitingForNetwork(startId, data, cover)
								delay(DOWNLOAD_ERROR_DELAY)
								connectivityManager.waitForNetwork()
								retryCounter++
							} else {
								throw e
							}
						}
					}

					outState.value = DownloadState.Progress(
						startId, data, cover,
						totalChapters = chapters.size,
						currentChapter = chapterIndex,
						totalPages = pages.size,
						currentPage = pageIndex,
					)

					if (settings.isDownloadsSlowdownEnabled) {
						delay(SLOWDOWN_DELAY)
					}
				}
			}
			outState.value = DownloadState.PostProcessing(startId, data, cover)
			output.mergeWithExisting()
			output.finalize()
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
				File(destination, tempFileName).deleteAwait()
			}
			coroutineContext[WakeLockNode]?.release()
			semaphore.release()
		}
	}

	private suspend fun downloadFile(url: String, referer: String, destination: File, tempFileName: String): File {
		val request = Request.Builder()
			.url(url)
			.header(CommonHeaders.REFERER, referer)
			.cacheControl(CommonHeaders.CACHE_CONTROL_DISABLED)
			.get()
			.build()
		val call = okHttp.newCall(request)
		val file = File(destination, tempFileName)
		val response = call.clone().await()
		runInterruptible(Dispatchers.IO) {
			file.outputStream().use { out ->
				checkNotNull(response.body).byteStream().copyTo(out)
			}
		}
		return file
	}

	private fun errorStateHandler(outState: MutableStateFlow<DownloadState>) =
		CoroutineExceptionHandler { _, throwable ->
			val prevValue = outState.value
			outState.value = DownloadState.Error(
				startId = prevValue.startId,
				manga = prevValue.manga,
				cover = prevValue.cover,
				error = throwable,
			)
		}

	class Factory(
		private val context: Context,
		private val imageLoader: ImageLoader,
		private val okHttp: OkHttpClient,
		private val cache: PagesCache,
		private val localMangaRepository: LocalMangaRepository,
		private val settings: AppSettings,
	) {

		fun create(coroutineScope: CoroutineScope) = DownloadManager(
			coroutineScope = coroutineScope,
			context = context,
			imageLoader = imageLoader,
			okHttp = okHttp,
			cache = cache,
			localMangaRepository = localMangaRepository,
			settings = settings,
		)
	}
}