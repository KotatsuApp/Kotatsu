package org.koitharu.kotatsu.download.ui.worker

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.internal.closeQuietly
import okio.IOException
import okio.buffer
import okio.sink
import okio.use
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.image.BitmapDecoderCompat
import org.koitharu.kotatsu.core.model.ids
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.network.imageproxy.ImageProxyInterceptor
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.MimeTypes
import org.koitharu.kotatsu.core.util.Throttler
import org.koitharu.kotatsu.core.util.ext.MimeType
import org.koitharu.kotatsu.core.util.ext.awaitFinishedWorkInfosByTag
import org.koitharu.kotatsu.core.util.ext.awaitUpdateWork
import org.koitharu.kotatsu.core.util.ext.awaitWorkInfosByTag
import org.koitharu.kotatsu.core.util.ext.deleteAwait
import org.koitharu.kotatsu.core.util.ext.deleteWork
import org.koitharu.kotatsu.core.util.ext.deleteWorks
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.getWorkInputData
import org.koitharu.kotatsu.core.util.ext.getWorkSpec
import org.koitharu.kotatsu.core.util.ext.openSource
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.core.util.ext.toMimeType
import org.koitharu.kotatsu.core.util.ext.toMimeTypeOrNull
import org.koitharu.kotatsu.core.util.ext.withTicker
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.core.util.progress.RealtimeEtaEstimator
import org.koitharu.kotatsu.download.domain.DownloadProgress
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.LocalStorageCache
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.data.PageCache
import org.koitharu.kotatsu.local.data.TempFileFilter
import org.koitharu.kotatsu.local.data.input.LocalMangaParser
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.domain.MangaLock
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.exception.TooManyRequestExceptions
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.requireBody
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.domain.PageLoader
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	@MangaHttpClient private val okHttp: OkHttpClient,
	@PageCache private val cache: LocalStorageCache,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaLock: MangaLock,
	private val mangaDataRepository: MangaDataRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val settings: AppSettings,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
	private val slowdownDispatcher: DownloadSlowdownDispatcher,
	private val imageProxyInterceptor: ImageProxyInterceptor,
	notificationFactoryFactory: DownloadNotificationFactory.Factory,
) : CoroutineWorker(appContext, params) {

	private val task = DownloadTask(params.inputData)
	private val notificationFactory = notificationFactoryFactory.create(uuid = params.id, isSilent = task.isSilent)
	private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	@Volatile
	private var lastPublishedState: DownloadState? = null
	private val currentState: DownloadState
		get() = checkNotNull(lastPublishedState)

	private val etaEstimator = RealtimeEtaEstimator()
	private val notificationThrottler = Throttler(400)

	override suspend fun doWork(): Result {
		setForeground(getForegroundInfo())
		val manga = mangaDataRepository.findMangaById(task.mangaId, withChapters = true) ?: return Result.failure()
		publishState(DownloadState(manga = manga, isIndeterminate = true).also { lastPublishedState = it })
		val downloadedIds = getDoneChapters(manga)
		return try {
			val pausingHandle = PausingHandle()
			if (task.isPaused) {
				pausingHandle.pause()
			}
			withContext(pausingHandle) {
				downloadMangaImpl(manga, task, downloadedIds)
			}
			Result.success(currentState.toWorkData())
		} catch (_: CancellationException) {
			withContext(NonCancellable) {
				val notification = notificationFactory.create(currentState.copy(isStopped = true))
				notificationManager.notify(id.hashCode(), notification)
			}
			Result.failure(
				currentState.copy(eta = -1L, isStuck = false).toWorkData(),
			)
		} catch (e: Exception) {
			e.printStackTraceDebug()
			Result.failure(
				currentState.copy(
					error = e,
					errorMessage = e.getDisplayMessage(applicationContext.resources),
					eta = -1L,
					isStuck = false,
				).toWorkData(),
			)
		} finally {
			notificationManager.cancel(id.hashCode())
		}
	}

	override suspend fun getForegroundInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		ForegroundInfo(
			id.hashCode(),
			notificationFactory.create(lastPublishedState),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	} else {
		ForegroundInfo(
			id.hashCode(),
			notificationFactory.create(lastPublishedState),
		)
	}

	private suspend fun downloadMangaImpl(
		subject: Manga,
		task: DownloadTask,
		excludedIds: Set<Long>,
	) {
		var manga = subject
		val chaptersToSkip = excludedIds.toMutableSet()
		val pausingReceiver = PausingReceiver(id, PausingHandle.current())
		mangaLock.withLock(manga) {
			ContextCompat.registerReceiver(
				applicationContext,
				pausingReceiver,
				PausingReceiver.createIntentFilter(id),
				ContextCompat.RECEIVER_NOT_EXPORTED,
			)
			val destination = localMangaRepository.getOutputDir(manga, task.destination)
			checkNotNull(destination) { applicationContext.getString(R.string.cannot_find_available_storage) }
			var output: LocalMangaOutput? = null
			try {
				if (manga.isLocal) {
					manga = localMangaRepository.getRemoteManga(manga)
						?: error("Cannot obtain remote manga instance")
				}
				val repo = mangaRepositoryFactory.create(manga.source)
				val mangaDetails = if (manga.chapters.isNullOrEmpty() || manga.description.isNullOrEmpty()) repo.getDetails(manga) else manga
				output = LocalMangaOutput.getOrCreate(
					root = destination,
					manga = mangaDetails,
					format = task.format ?: settings.preferredDownloadFormat,
				)
				val coverUrl = mangaDetails.largeCoverUrl.ifNullOrEmpty { mangaDetails.coverUrl }
				if (!coverUrl.isNullOrEmpty()) {
					downloadFile(coverUrl, destination, repo.source).let { file ->
						output.addCover(file, getMediaType(coverUrl, file))
						file.deleteAwait()
					}
				}
				val chapters = getChapters(mangaDetails, task)
				for ((chapterIndex, chapter) in chapters.withIndex()) {
					checkIsPaused()
					if (chaptersToSkip.remove(chapter.value.id)) {
						publishState(currentState.copy(downloadedChapters = currentState.downloadedChapters + 1))
						continue
					}
					val pages = runFailsafe {
						repo.getPages(chapter.value)
					} ?: continue
					val pageCounter = AtomicInteger(0)
					channelFlow {
						val semaphore = Semaphore(MAX_PAGES_PARALLELISM)
						for ((pageIndex, page) in pages.withIndex()) {
							checkIsPaused()
							launch {
								semaphore.withPermit {
									runFailsafe {
										val url = repo.getPageUrl(page)
										val file = cache[url]
											?: downloadFile(url, destination, repo.source)
										output.addPage(
											chapter = chapter,
											file = file,
											pageNumber = pageIndex,
											type = getMediaType(url, file),
										)
										if (file.extension == "tmp") {
											file.deleteAwait()
										}
									}
									send(pageIndex)
								}
							}
						}
					}.map {
						DownloadProgress(
							totalChapters = chapters.size,
							currentChapter = chapterIndex,
							totalPages = pages.size,
							currentPage = pageCounter.getAndIncrement(),
						)
					}.withTicker(2L, TimeUnit.SECONDS).collect { progress ->
						publishState(
							currentState.copy(
								totalChapters = progress.totalChapters,
								currentChapter = progress.currentChapter,
								totalPages = progress.totalPages,
								currentPage = progress.currentPage,
								isIndeterminate = false,
								eta = etaEstimator.getEta(),
								isStuck = etaEstimator.isStuck(),
							),
						)
					}
					if (output.flushChapter(chapter.value)) {
						runCatchingCancellable {
							localStorageChanges.emit(LocalMangaParser(output.rootFile).getManga(withDetails = false))
						}.onFailure(Throwable::printStackTraceDebug)
					}
					publishState(currentState.copy(downloadedChapters = currentState.downloadedChapters + 1))
				}
				publishState(currentState.copy(isIndeterminate = true, eta = -1L, isStuck = false))
				output.mergeWithExisting()
				output.finish()
				val localManga = LocalMangaParser(output.rootFile).getManga(withDetails = false)
				localStorageChanges.emit(localManga)
				publishState(currentState.copy(localManga = localManga, eta = -1L, isStuck = false))
			} catch (e: Exception) {
				if (e !is CancellationException) {
					publishState(
						currentState.copy(
							error = e,
							errorMessage = e.getDisplayMessage(applicationContext.resources),
						),
					)
				}
				throw e
			} finally {
				withContext(NonCancellable) {
					applicationContext.unregisterReceiver(pausingReceiver)
					output?.closeQuietly()
					output?.cleanup()
					destination.listFiles(TempFileFilter())?.forEach {
						it.deleteAwait()
					}
				}
			}
		}
	}

	private suspend fun <R> runFailsafe(
		block: suspend () -> R,
	): R? {
		checkIsPaused()
		var countDown = MAX_FAILSAFE_ATTEMPTS
		failsafe@ while (true) {
			try {
				return block()
			} catch (e: IOException) {
				val retryDelay = if (e is TooManyRequestExceptions) {
					e.getRetryDelay()
				} else {
					DOWNLOAD_ERROR_DELAY
				}
				if (countDown <= 0 || retryDelay < 0 || retryDelay > MAX_RETRY_DELAY) {
					val pausingHandle = PausingHandle.current()
					if (pausingHandle.skipAllErrors()) {
						return null
					}
					publishState(
						currentState.copy(
							isPaused = true,
							error = e,
							errorMessage = e.getDisplayMessage(applicationContext.resources),
							eta = -1L,
							isStuck = false,
						),
					)
					countDown = MAX_FAILSAFE_ATTEMPTS
					pausingHandle.pause()
					try {
						pausingHandle.awaitResumed()
						if (pausingHandle.skipCurrentError()) {
							return null
						}
					} finally {
						publishState(currentState.copy(isPaused = false, error = null, errorMessage = null))
					}
				} else {
					countDown--
					delay(retryDelay)
				}
			}
		}
	}

	private suspend fun checkIsPaused() {
		val pausingHandle = PausingHandle.current()
		if (pausingHandle.isPaused) {
			publishState(currentState.copy(isPaused = true, eta = -1L, isStuck = false))
			try {
				pausingHandle.awaitResumed()
			} finally {
				publishState(currentState.copy(isPaused = false))
			}
		}
	}

	private suspend fun getMediaType(url: String, file: File): MimeType? = runInterruptible(Dispatchers.IO) {
		BitmapDecoderCompat.probeMimeType(file)?.let {
			return@runInterruptible it
		}
		MimeTypes.getMimeTypeFromUrl(url)
	}

	private suspend fun downloadFile(
		url: String,
		destination: File,
		source: MangaSource,
	): File {
		if (url.startsWith("content:", ignoreCase = true) || url.startsWith("file:", ignoreCase = true)) {
			val uri = url.toUri()
			val cr = applicationContext.contentResolver
			val ext = uri.toFileOrNull()?.let {
				MimeTypes.getNormalizedExtension(it.name)
			} ?: cr.getType(uri)?.toMimeTypeOrNull()?.let { MimeTypes.getExtension(it) }
			val file = destination.createTempFile(ext)
			try {
				cr.openSource(uri).use { input ->
					file.sink(append = false).buffer().use {
						it.writeAllCancellable(input)
					}
				}
			} catch (e: Exception) {
				file.delete()
				throw e
			}
			return file
		}
		val request = PageLoader.createPageRequest(url, source)
		slowdownDispatcher.delay(source)
		return imageProxyInterceptor.interceptPageRequest(request, okHttp)
			.ensureSuccess()
			.use { response ->
				var file: File? = null
				try {
					response.requireBody().use { body ->
						file = destination.createTempFile(
							ext = MimeTypes.getExtension(body.contentType()?.toMimeType())
						)
						file.sink(append = false).buffer().use {
							it.writeAllCancellable(body.source())
						}
					}
				} catch (e: Exception) {
					file?.delete()
					throw e
				}
				checkNotNull(file)
			}
	}

	private fun File.createTempFile(ext: String?) = File(
		this,
		buildString {
			append(UUID.randomUUID().toString())
			if (!ext.isNullOrEmpty()) {
				append('.')
				append(ext)
			}
			append(".tmp")
		},
	)

	private suspend fun publishState(state: DownloadState) {
		val previousState = currentState
		lastPublishedState = state
		if (previousState.isParticularProgress && state.isParticularProgress) {
			etaEstimator.onProgressChanged(state.progress, state.max)
		} else {
			etaEstimator.reset()
			notificationThrottler.reset()
		}
		val notification = notificationFactory.create(state)
		if (state.isFinalState) {
			if (!notificationFactory.isSilent) {
				notificationManager.notify(id.toString(), id.hashCode(), notification)
			}
		} else if (notificationThrottler.throttle()) {
			notificationManager.notify(id.hashCode(), notification)
		} else {
			return
		}
		setProgress(state.toWorkData())
	}

	private suspend fun getDoneChapters(manga: Manga) = runCatchingCancellable {
		localMangaRepository.getDetails(manga).chapters?.ids()
	}.getOrNull().orEmpty()

	private fun getChapters(
		manga: Manga,
		task: DownloadTask,
	): List<IndexedValue<MangaChapter>> {
		val chapters = checkNotNull(manga.chapters) { "Chapters list must not be null" }
		val chaptersIdsSet = task.chaptersIds?.toMutableSet()
		val result = ArrayList<IndexedValue<MangaChapter>>((chaptersIdsSet ?: chapters).size)
		val counters = HashMap<String?, Int>()
		for (chapter in chapters) {
			val index = counters[chapter.branch] ?: 0
			counters[chapter.branch] = index + 1
			if (chaptersIdsSet != null && !chaptersIdsSet.remove(chapter.id)) {
				continue
			}
			result.add(IndexedValue(index, chapter))
		}
		if (chaptersIdsSet != null) {
			check(chaptersIdsSet.isEmpty()) {
				"${chaptersIdsSet.size} of ${task.chaptersIds.size} requested chapters not found in manga"
			}
		}
		check(result.isNotEmpty()) { "Chapters list must not be empty" }
		return result
	}

	@Reusable
	class Scheduler @Inject constructor(
		@ApplicationContext private val context: Context,
		private val mangaDataRepository: MangaDataRepository,
		private val workManager: WorkManager,
	) {

		fun observeWorks(): Flow<List<WorkInfo>> = workManager
			.getWorkInfosByTagFlow(TAG)

		@SuppressLint("RestrictedApi")
		suspend fun getInputData(id: UUID): Data? {
			val spec = workManager.getWorkSpec(id) ?: return null
			return Data.Builder()
				.putAll(spec.input)
				.putLong(DownloadState.DATA_TIMESTAMP, spec.scheduleRequestedAt)
				.build()
		}

		suspend fun getTask(workId: UUID): DownloadTask? {
			return workManager.getWorkInputData(workId)?.let { DownloadTask(it) }
		}

		suspend fun cancel(id: UUID) {
			workManager.cancelWorkById(id).await()
		}

		suspend fun cancelAll() {
			workManager.cancelAllWorkByTag(TAG).await()
		}

		fun pause(id: UUID) = context.sendBroadcast(
			PausingReceiver.getPauseIntent(context, id),
		)

		fun resume(id: UUID) = context.sendBroadcast(
			PausingReceiver.getResumeIntent(context, id),
		)

		fun skip(id: UUID) = context.sendBroadcast(
			PausingReceiver.getSkipIntent(context, id),
		)

		fun skipAll(id: UUID) = context.sendBroadcast(
			PausingReceiver.getSkipAllIntent(context, id),
		)

		suspend fun delete(id: UUID) {
			workManager.deleteWork(id)
		}

		suspend fun delete(ids: Collection<UUID>) {
			val wm = workManager
			ids.forEach { id -> wm.cancelWorkById(id).await() }
			workManager.deleteWorks(ids)
		}

		suspend fun removeCompleted() {
			val finishedWorks = workManager.awaitFinishedWorkInfosByTag(TAG)
			workManager.deleteWorks(finishedWorks.mapToSet { it.id })
		}

		suspend fun updateConstraints(allowMeteredNetwork: Boolean) {
			val constraints = createConstraints(allowMeteredNetwork)
			val works = workManager.awaitWorkInfosByTag(TAG)
			for (work in works) {
				if (work.state.isFinished) {
					continue
				}
				val request = OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(constraints)
					.addTag(TAG)
					.setId(work.id)
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
				workManager.awaitUpdateWork(request)
			}
		}

		suspend fun schedule(tasks: Collection<Pair<Manga, DownloadTask>>) {
			if (tasks.isEmpty()) {
				return
			}
			val requests = tasks.map { (manga, task) ->
				mangaDataRepository.storeManga(manga, replaceExisting = true)
				OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(createConstraints(task.allowMeteredNetwork))
					.addTag(TAG)
					.keepResultsForAtLeast(30, TimeUnit.DAYS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
					.setInputData(task.toData())
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
			}
			workManager.enqueue(requests).await()
		}

		private fun createConstraints(allowMeteredNetwork: Boolean) = Constraints.Builder()
			.setRequiredNetworkType(if (allowMeteredNetwork) NetworkType.CONNECTED else NetworkType.UNMETERED)
			.build()
	}

	private companion object {

		const val MAX_FAILSAFE_ATTEMPTS = 2
		const val MAX_PAGES_PARALLELISM = 4
		const val DOWNLOAD_ERROR_DELAY = 2_000L
		const val MAX_RETRY_DELAY = 7_200_000L // 2 hours
		const val TAG = "download"
	}
}
