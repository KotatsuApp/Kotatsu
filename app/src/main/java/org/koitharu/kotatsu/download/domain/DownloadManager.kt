package org.koitharu.kotatsu.download.domain

import android.app.Service
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.IOException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.download.ui.service.PausingHandle
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.utils.ext.copyToSuspending
import org.koitharu.kotatsu.utils.ext.deleteAwait
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import org.koitharu.kotatsu.utils.progress.PausingProgressJob
import java.io.File
import javax.inject.Inject

private const val MAX_FAILSAFE_ATTEMPTS = 2
private const val DOWNLOAD_ERROR_DELAY = 500L
private const val SLOWDOWN_DELAY = 200L

@ServiceScoped
class DownloadManager @Inject constructor(
	service: Service,
	@ApplicationContext private val context: Context,
	private val imageLoader: ImageLoader,
	private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val localMangaRepository: LocalMangaRepository,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	private val coverWidth = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_width,
	)
	private val coverHeight = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_height,
	)
	private val semaphore = Semaphore(settings.downloadsParallelism)
	private val coroutineScope = (service as LifecycleService).lifecycleScope

	fun downloadManga(
		manga: Manga,
		chaptersIds: LongArray?,
		startId: Int,
	): PausingProgressJob<DownloadState> {
		val stateFlow = MutableStateFlow<DownloadState>(
			DownloadState.Queued(startId = startId, manga = manga, cover = null),
		)
		val pausingHandle = PausingHandle()
		val job = coroutineScope.launch(Dispatchers.Default + errorStateHandler(stateFlow)) {
			try {
				downloadMangaImpl(manga, chaptersIds?.takeUnless { it.isEmpty() }, stateFlow, pausingHandle, startId)
			} catch (e: CancellationException) { // handle cancellation if not handled already
				val state = stateFlow.value
				if (state !is DownloadState.Cancelled) {
					stateFlow.value = DownloadState.Cancelled(startId, state.manga, state.cover)
				}
				throw e
			}
		}
		return PausingProgressJob(job, stateFlow, pausingHandle)
	}

	private suspend fun downloadMangaImpl(
		manga: Manga,
		chaptersIds: LongArray?,
		outState: MutableStateFlow<DownloadState>,
		pausingHandle: PausingHandle,
		startId: Int,
	) {
		@Suppress("NAME_SHADOWING")
		var manga = manga
		val chaptersIdsSet = chaptersIds?.toMutableSet()
		val cover = loadCover(manga)
		outState.value = DownloadState.Queued(startId, manga, cover)
		withMangaLock(manga) {
			semaphore.withPermit {
				outState.value = DownloadState.Preparing(startId, manga, null)
				val destination = localMangaRepository.getOutputDir()
				checkNotNull(destination) { context.getString(R.string.cannot_find_available_storage) }
				val tempFileName = "${manga.id}_$startId.tmp"
				var output: LocalMangaOutput? = null
				try {
					if (manga.source == MangaSource.LOCAL) {
						manga = localMangaRepository.getRemoteManga(manga)
							?: error("Cannot obtain remote manga instance")
					}
					val repo = mangaRepositoryFactory.create(manga.source)
					outState.value = DownloadState.Preparing(startId, manga, cover)
					val data = if (manga.chapters.isNullOrEmpty()) repo.getDetails(manga) else manga
					output = LocalMangaOutput.getOrCreate(destination, data)
					val coverUrl = data.largeCoverUrl ?: data.coverUrl
					downloadFile(coverUrl, destination, tempFileName, repo.source).let { file ->
						output.addCover(file, MimeTypeMap.getFileExtensionFromUrl(coverUrl))
					}
					val chapters = checkNotNull(
						if (chaptersIdsSet == null) {
							data.chapters
						} else {
							data.chapters?.filter { x -> chaptersIdsSet.remove(x.id) }
						},
					) { "Chapters list must not be null" }
					check(chapters.isNotEmpty()) { "Chapters list must not be empty" }
					check(chaptersIdsSet.isNullOrEmpty()) {
						"${chaptersIdsSet?.size} of ${chaptersIds?.size} requested chapters not found in manga"
					}
					for ((chapterIndex, chapter) in chapters.withIndex()) {
						val pages = runFailsafe(outState, pausingHandle) {
							repo.getPages(chapter)
						}
						for ((pageIndex, page) in pages.withIndex()) {
							runFailsafe(outState, pausingHandle) {
								val url = repo.getPageUrl(page)
								val file = cache.get(url)
									?: downloadFile(url, destination, tempFileName, repo.source)
								output.addPage(
									chapter = chapter,
									file = file,
									pageNumber = pageIndex,
									ext = MimeTypeMap.getFileExtensionFromUrl(url),
								)
							}
							outState.value = DownloadState.Progress(
								startId = startId,
								manga = data,
								cover = cover,
								totalChapters = chapters.size,
								currentChapter = chapterIndex,
								totalPages = pages.size,
								currentPage = pageIndex,
							)

							if (settings.isDownloadsSlowdownEnabled) {
								delay(SLOWDOWN_DELAY)
							}
						}
						output.flushChapter(chapter)
					}
					outState.value = DownloadState.PostProcessing(startId, data, cover)
					output.mergeWithExisting()
					output.finish()
					val localManga = LocalMangaInput.of(output.rootFile).getManga().manga
					outState.value = DownloadState.Done(startId, data, cover, localManga)
				} catch (e: CancellationException) {
					outState.value = DownloadState.Cancelled(startId, manga, cover)
					throw e
				} catch (e: Throwable) {
					e.printStackTraceDebug()
					outState.value = DownloadState.Error(startId, manga, cover, e, false)
				} finally {
					withContext(NonCancellable) {
						output?.closeQuietly()
						output?.cleanup()
						File(destination, tempFileName).deleteAwait()
					}
				}
			}
		}
	}

	private suspend fun <R> runFailsafe(
		outState: MutableStateFlow<DownloadState>,
		pausingHandle: PausingHandle,
		block: suspend () -> R,
	): R {
		var countDown = MAX_FAILSAFE_ATTEMPTS
		failsafe@ while (true) {
			try {
				return block()
			} catch (e: IOException) {
				if (countDown <= 0) {
					val state = outState.value
					outState.value = DownloadState.Error(state.startId, state.manga, state.cover, e, true)
					countDown = MAX_FAILSAFE_ATTEMPTS
					pausingHandle.pause()
					pausingHandle.awaitResumed()
					outState.value = state
				} else {
					countDown--
					delay(DOWNLOAD_ERROR_DELAY)
				}
			}
		}
	}

	private suspend fun downloadFile(
		url: String,
		destination: File,
		tempFileName: String,
		source: MangaSource,
	): File {
		val request = Request.Builder()
			.url(url)
			.tag(MangaSource::class.java, source)
			.cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
			.get()
			.build()
		val call = okHttp.newCall(request)
		val file = File(destination, tempFileName)
		val response = call.clone().await()
		file.outputStream().use { out ->
			checkNotNull(response.body).byteStream().copyToSuspending(out)
		}
		return file
	}

	private fun errorStateHandler(outState: MutableStateFlow<DownloadState>) =
		CoroutineExceptionHandler { _, throwable ->
			throwable.printStackTraceDebug()
			val prevValue = outState.value
			outState.value = DownloadState.Error(
				startId = prevValue.startId,
				manga = prevValue.manga,
				cover = prevValue.cover,
				error = throwable,
				canRetry = false,
			)
		}

	private suspend fun loadCover(manga: Manga) = runCatchingCancellable {
		imageLoader.execute(
			ImageRequest.Builder(context)
				.data(manga.coverUrl)
				.tag(manga.source)
				.size(coverWidth, coverHeight)
				.scale(Scale.FILL)
				.build(),
		).drawable
	}.getOrNull()

	private suspend inline fun <T> withMangaLock(manga: Manga, block: () -> T) = try {
		localMangaRepository.lockManga(manga.id)
		block()
	} finally {
		localMangaRepository.unlockManga(manga.id)
	}
}
