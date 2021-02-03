package org.koitharu.kotatsu.download

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.PowerManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseService
import org.koitharu.kotatsu.base.ui.dialog.CheckBoxAlertDialog
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.MangaZip
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.math.absoluteValue

class DownloadService : BaseService() {

	private lateinit var notification: DownloadNotification
	private lateinit var wakeLock: PowerManager.WakeLock
	private lateinit var connectivityManager: ConnectivityManager

	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()
	private val settings by inject<AppSettings>()
	private val imageLoader by inject<ImageLoader>()
	private val jobs = HashMap<Int, Job>()
	private val mutex = Mutex()

	override fun onCreate() {
		super.onCreate()
		notification = DownloadNotification(this)
		connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kotatsu:downloading")
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		when (intent?.action) {
			ACTION_DOWNLOAD_START -> {
				val manga = intent.getParcelableExtra<Manga>(EXTRA_MANGA)
				val chapters = intent.getLongArrayExtra(EXTRA_CHAPTERS_IDS)?.toArraySet()
				if (manga != null) {
					jobs[startId] = downloadManga(manga, chapters, startId)
					Toast.makeText(this, R.string.manga_downloading_, Toast.LENGTH_SHORT).show()
				} else {
					stopSelf(startId)
				}
			}
			ACTION_DOWNLOAD_CANCEL -> {
				val cancelId = intent.getIntExtra(EXTRA_CANCEL_ID, 0)
				jobs.remove(cancelId)?.cancel()
				stopSelf(startId)
			}
			else -> stopSelf(startId)
		}
		return START_NOT_STICKY
	}

	private fun downloadManga(manga: Manga, chaptersIds: Set<Long>?, startId: Int): Job {
		return lifecycleScope.launch(Dispatchers.Default) {
			mutex.lock()
			wakeLock.acquire(TimeUnit.HOURS.toMillis(1))
			notification.fillFrom(manga)
			notification.setCancelId(startId)
			withContext(Dispatchers.Main.immediate) {
				startForeground(DownloadNotification.NOTIFICATION_ID, notification())
			}
			val destination = settings.getStorageDir(this@DownloadService)
			checkNotNull(destination) { getString(R.string.cannot_find_available_storage) }
			var output: MangaZip? = null
			try {
				val repo = mangaRepositoryOf(manga.source)
				val cover = runCatching {
					imageLoader.execute(
						ImageRequest.Builder(this@DownloadService)
							.data(manga.coverUrl)
							.build()
					).drawable
				}.getOrNull()
				notification.setLargeIcon(cover)
				notification.update()
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
									notification.setWaitingForNetwork()
									notification.update()
									connectivityManager.waitForNetwork()
									continue@failsafe
								}
							} while (false)
							notification.setProgress(
								chapters.size,
								pages.size,
								chapterIndex,
								pageIndex
							)
							notification.update()
						}
					}
				}
				notification.setCancelId(0)
				notification.setPostProcessing()
				notification.update()
				if (!output.compress()) {
					throw RuntimeException("Cannot create target file")
				}
				val result = get<LocalMangaRepository>().getFromFile(output.file)
				notification.setDone(result)
				notification.dismiss()
				notification.update(manga.id.toInt().absoluteValue)
			} catch (_: CancellationException) {
				withContext(NonCancellable) {
					notification.setCancelling()
					notification.setCancelId(0)
					notification.update()
				}
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				notification.setError(e)
				notification.setCancelId(0)
				notification.dismiss()
				notification.update(manga.id.toInt().absoluteValue)
			} finally {
				withContext(NonCancellable) {
					jobs.remove(startId)
					output?.cleanup()
					destination.sub("page.tmp").delete()
					withContext(Dispatchers.Main.immediate) {
						stopForeground(true)
						notification.dismiss()
						stopSelf(startId)
					}
					if (wakeLock.isHeld) {
						wakeLock.release()
					}
					mutex.unlock()
				}
			}
		}
	}

	private suspend fun downloadFile(url: String, referer: String, destination: File): File {
		val request = Request.Builder()
			.url(url)
			.header(CommonHeaders.REFERER, referer)
			.cacheControl(CacheUtils.CONTROL_DISABLED)
			.get()
			.build()
		return retryUntilSuccess(3) {
			okHttp.newCall(request).await().use { response ->
				val file = destination.sub("page.tmp")
				file.outputStream().use { out ->
					response.body!!.byteStream().copyTo(out)
				}
				file
			}
		}
	}

	companion object {

		private const val ACTION_DOWNLOAD_START =
			"${BuildConfig.APPLICATION_ID}.action.ACTION_DOWNLOAD_START"
		private const val ACTION_DOWNLOAD_CANCEL =
			"${BuildConfig.APPLICATION_ID}.action.ACTION_DOWNLOAD_CANCEL"

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTERS_IDS = "chapters_ids"
		private const val EXTRA_CANCEL_ID = "cancel_id"

		fun start(context: Context, manga: Manga, chaptersIds: Collection<Long>? = null) {
			confirmDataTransfer(context) {
				val intent = Intent(context, DownloadService::class.java)
				intent.action = ACTION_DOWNLOAD_START
				intent.putExtra(EXTRA_MANGA, manga)
				if (chaptersIds != null) {
					intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toLongArray())
				}
				ContextCompat.startForegroundService(context, intent)
			}
		}

		fun getCancelIntent(context: Context, startId: Int) =
			Intent(context, DownloadService::class.java)
				.setAction(ACTION_DOWNLOAD_CANCEL)
				.putExtra(ACTION_DOWNLOAD_CANCEL, startId)

		private fun confirmDataTransfer(context: Context, callback: () -> Unit) {
			val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			val settings = GlobalContext.get().get<AppSettings>()
			if (cm.isActiveNetworkMetered && settings.isTrafficWarningEnabled) {
				CheckBoxAlertDialog.Builder(context)
					.setTitle(R.string.warning)
					.setMessage(R.string.network_consumption_warning)
					.setCheckBoxText(R.string.dont_ask_again)
					.setCheckBoxChecked(false)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string._continue) { _, doNotAsk ->
						settings.isTrafficWarningEnabled = !doNotAsk
						callback()
					}.create()
					.show()
			} else {
				callback()
			}
		}
	}
}