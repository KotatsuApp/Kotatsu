package org.koitharu.kotatsu.download.ui.worker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.IOException
import okio.buffer
import okio.sink
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.TooManyRequestExceptions
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.Throttler
import org.koitharu.kotatsu.core.util.ext.awaitFinishedWorkInfosByTag
import org.koitharu.kotatsu.core.util.ext.awaitUpdateWork
import org.koitharu.kotatsu.core.util.ext.awaitWorkInfoById
import org.koitharu.kotatsu.core.util.ext.awaitWorkInfosByTag
import org.koitharu.kotatsu.core.util.ext.deleteAwait
import org.koitharu.kotatsu.core.util.ext.deleteWork
import org.koitharu.kotatsu.core.util.ext.deleteWorks
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.writeAllCancellable
import org.koitharu.kotatsu.core.util.progress.TimeLeftEstimator
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.data.input.LocalMangaInput
import org.koitharu.kotatsu.local.data.output.LocalMangaOutput
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	@MangaHttpClient private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaDataRepository: MangaDataRepository,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
	notificationFactoryFactory: DownloadNotificationFactory.Factory,
) : CoroutineWorker(appContext, params) {

	private val notificationFactory = notificationFactoryFactory.create(params.id)
	private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	@Volatile
	private var lastPublishedState: DownloadState? = null
	private val currentState: DownloadState
		get() = checkNotNull(lastPublishedState)

	private val pausingHandle = PausingHandle()
	private val timeLeftEstimator = TimeLeftEstimator()
	private val notificationThrottler = Throttler(400)
	private val pausingReceiver = PausingReceiver(params.id, pausingHandle)

	override suspend fun doWork(): Result {
		setForeground(getForegroundInfo())
		val mangaId = inputData.getLong(MANGA_ID, 0L)
		val manga = mangaDataRepository.findMangaById(mangaId) ?: return Result.failure()
		val chaptersIds = inputData.getLongArray(CHAPTERS_IDS)?.takeUnless { it.isEmpty() }
		val downloadedIds = getDoneChapters()
		lastPublishedState = DownloadState(manga, isIndeterminate = true)
		return try {
			downloadMangaImpl(chaptersIds, downloadedIds)
			Result.success(currentState.toWorkData())
		} catch (e: CancellationException) {
			withContext(NonCancellable) {
				val notification = notificationFactory.create(currentState.copy(isStopped = true))
				notificationManager.notify(id.hashCode(), notification)
			}
			throw e
		} catch (e: IOException) {
			e.printStackTraceDebug()
			Result.retry()
		} catch (e: Exception) {
			e.printStackTraceDebug()
			Result.failure(
				currentState.copy(
					error = e.getDisplayMessage(applicationContext.resources),
					eta = -1L,
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
		includedIds: LongArray?,
		excludedIds: LongArray,
	) {
		var manga = currentState.manga
		val chaptersToSkip = excludedIds.toMutableSet()
		withMangaLock(manga) {
			ContextCompat.registerReceiver(
				applicationContext,
				pausingReceiver,
				PausingReceiver.createIntentFilter(id),
				ContextCompat.RECEIVER_NOT_EXPORTED,
			)
			val destination = localMangaRepository.getOutputDir(manga)
			checkNotNull(destination) { applicationContext.getString(R.string.cannot_find_available_storage) }
			val tempFileName = "${manga.id}_$id.tmp"
			var output: LocalMangaOutput? = null
			try {
				if (manga.source == MangaSource.LOCAL) {
					manga = localMangaRepository.getRemoteManga(manga)
						?: error("Cannot obtain remote manga instance")
				}
				val repo = mangaRepositoryFactory.create(manga.source)
				val mangaDetails = if (manga.chapters.isNullOrEmpty()) repo.getDetails(manga) else manga
				output = LocalMangaOutput.getOrCreate(destination, mangaDetails)
				val coverUrl = mangaDetails.largeCoverUrl.ifNullOrEmpty { mangaDetails.coverUrl }
				if (coverUrl.isNotEmpty()) {
					downloadFile(coverUrl, destination, tempFileName, repo.source).let { file ->
						output.addCover(file, MimeTypeMap.getFileExtensionFromUrl(coverUrl))
					}
				}
				val chapters = getChapters(mangaDetails, includedIds)
				for ((chapterIndex, chapter) in chapters.withIndex()) {
					if (chaptersToSkip.remove(chapter.id)) {
						publishState(
							currentState.copy(
								downloadedChapters = currentState.downloadedChapters + chapter.id,
							),
						)
						continue
					}
					val pages = runFailsafe(pausingHandle) {
						repo.getPages(chapter)
					}
					for ((pageIndex, page) in pages.withIndex()) {
						runFailsafe(pausingHandle) {
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
						publishState(
							currentState.copy(
								totalChapters = chapters.size,
								currentChapter = chapterIndex,
								totalPages = pages.size,
								currentPage = pageIndex,
								isIndeterminate = false,
								eta = timeLeftEstimator.getEta(),
							),
						)

						if (settings.isDownloadsSlowdownEnabled) {
							delay(SLOWDOWN_DELAY)
						}
					}
					if (output.flushChapter(chapter)) {
						runCatchingCancellable {
							localStorageChanges.emit(LocalMangaInput.of(output.rootFile).getManga())
						}.onFailure(Throwable::printStackTraceDebug)
					}
					publishState(
						currentState.copy(
							downloadedChapters = currentState.downloadedChapters + chapter.id,
						),
					)
				}
				publishState(currentState.copy(isIndeterminate = true, eta = -1L))
				output.mergeWithExisting()
				output.finish()
				val localManga = LocalMangaInput.of(output.rootFile).getManga()
				localStorageChanges.emit(localManga)
				publishState(currentState.copy(localManga = localManga, eta = -1L))
			} catch (e: Exception) {
				if (e !is CancellationException) {
					publishState(currentState.copy(error = e.getDisplayMessage(applicationContext.resources)))
				}
				throw e
			} finally {
				withContext(NonCancellable) {
					applicationContext.unregisterReceiver(pausingReceiver)
					output?.closeQuietly()
					output?.cleanup()
					File(destination, tempFileName).deleteAwait()
				}
			}
		}
	}

	private suspend fun <R> runFailsafe(
		pausingHandle: PausingHandle,
		block: suspend () -> R,
	): R {
		if (pausingHandle.isPaused) {
			publishState(currentState.copy(isPaused = true, eta = -1L))
			pausingHandle.awaitResumed()
			publishState(currentState.copy(isPaused = false))
		}
		var countDown = MAX_FAILSAFE_ATTEMPTS
		failsafe@ while (true) {
			try {
				return block()
			} catch (e: IOException) {
				if (countDown <= 0) {
					publishState(
						currentState.copy(
							isPaused = true,
							error = e.getDisplayMessage(applicationContext.resources),
							eta = -1L,
						),
					)
					countDown = MAX_FAILSAFE_ATTEMPTS
					pausingHandle.pause()
					pausingHandle.awaitResumed()
					publishState(currentState.copy(isPaused = false, error = null))
				} else {
					countDown--
					val retryDelay = if (e is TooManyRequestExceptions) {
						e.retryAfter + DOWNLOAD_ERROR_DELAY
					} else {
						DOWNLOAD_ERROR_DELAY
					}
					delay(retryDelay)
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
			.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
			.cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
			.get()
			.build()
		val call = okHttp.newCall(request)
		val file = File(destination, tempFileName)
		val response = call.clone().await()
		checkNotNull(response.body).use { body ->
			file.sink(append = false).buffer().use {
				it.writeAllCancellable(body.source())
			}
		}
		return file
	}

	private suspend fun publishState(state: DownloadState) {
		val previousState = currentState
		lastPublishedState = state
		if (previousState.isParticularProgress && state.isParticularProgress) {
			timeLeftEstimator.tick(state.progress, state.max)
		} else {
			timeLeftEstimator.emptyTick()
			notificationThrottler.reset()
		}
		val notification = notificationFactory.create(state)
		if (state.isFinalState) {
			notificationManager.notify(id.toString(), id.hashCode(), notification)
		} else if (notificationThrottler.throttle()) {
			notificationManager.notify(id.hashCode(), notification)
		} else {
			return
		}
		setProgress(state.toWorkData())
	}

	private suspend fun getDoneChapters(): LongArray {
		val work = WorkManager.getInstance(applicationContext).awaitWorkInfoById(id)
			?: return LongArray(0)
		return DownloadState.getDownloadedChapters(work.progress)
	}

	private fun getChapters(
		manga: Manga,
		includedIds: LongArray?,
	): List<MangaChapter> {
		val chapters = checkNotNull(manga.chapters) {
			"Chapters list must not be null"
		}.toMutableList()
		if (includedIds != null) {
			val chaptersIdsSet = includedIds.toMutableSet()
			chapters.retainAll { x -> chaptersIdsSet.remove(x.id) }
			check(chaptersIdsSet.isEmpty()) {
				"${chaptersIdsSet.size} of ${includedIds.size} requested chapters not found in manga"
			}
		}
		check(chapters.isNotEmpty()) { "Chapters list must not be empty" }
		return chapters
	}

	private suspend inline fun <T> withMangaLock(manga: Manga, block: () -> T) = try {
		localMangaRepository.lockManga(manga.id)
		block()
	} finally {
		localMangaRepository.unlockManga(manga.id)
	}

	@Reusable
	class Scheduler @Inject constructor(
		@ApplicationContext private val context: Context,
		private val workManager: WorkManager,
		private val dataRepository: MangaDataRepository,
		private val settings: AppSettings,
	) {

		suspend fun schedule(manga: Manga, chaptersIds: Collection<Long>?) {
			dataRepository.storeManga(manga)
			val data = Data.Builder()
				.putLong(MANGA_ID, manga.id)
			if (!chaptersIds.isNullOrEmpty()) {
				data.putLongArray(CHAPTERS_IDS, chaptersIds.toLongArray())
			}
			scheduleImpl(listOf(data.build()))
		}

		suspend fun schedule(manga: Collection<Manga>) {
			val data = manga.map {
				dataRepository.storeManga(it)
				Data.Builder()
					.putLong(MANGA_ID, it.id)
					.build()
			}
			scheduleImpl(data)
		}

		fun observeWorks(): Flow<List<WorkInfo>> = workManager
			.getWorkInfosByTagLiveData(TAG)
			.asFlow()

		suspend fun cancel(id: UUID) {
			workManager.cancelWorkById(id).await()
		}

		suspend fun cancelAll() {
			workManager.cancelAllWorkByTag(TAG).await()
		}

		fun pause(id: UUID) {
			val intent = PausingReceiver.getPauseIntent(context, id)
			context.sendBroadcast(intent)
		}

		fun resume(id: UUID) {
			val intent = PausingReceiver.getResumeIntent(context, id)
			context.sendBroadcast(intent)
		}

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

		suspend fun updateConstraints() {
			val constraints = createConstraints()
			val works = workManager.awaitWorkInfosByTag(TAG)
			for (work in works) {
				if (work.state.isFinished) {
					continue
				}
				val request = OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(constraints)
					.addTag(TAG)
					.setId(work.id)
					.build()
				workManager.awaitUpdateWork(request)
			}
		}

		private suspend fun scheduleImpl(data: Collection<Data>) {
			if (data.isEmpty()) {
				return
			}
			val constraints = createConstraints()
			val requests = data.map { inputData ->
				OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(constraints)
					.addTag(TAG)
					.keepResultsForAtLeast(30, TimeUnit.DAYS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
					.setInputData(inputData)
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
			}
			workManager.enqueue(requests).await()
		}

		private fun createConstraints() = Constraints.Builder()
			.setRequiredNetworkType(if (settings.isDownloadsWiFiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
			.build()
	}

	private companion object {

		const val MAX_FAILSAFE_ATTEMPTS = 2
		const val DOWNLOAD_ERROR_DELAY = 500L
		const val SLOWDOWN_DELAY = 100L
		const val MANGA_ID = "manga_id"
		const val CHAPTERS_IDS = "chapters"
		const val TAG = "download"
	}
}
