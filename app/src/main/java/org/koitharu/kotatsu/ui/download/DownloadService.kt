package org.koitharu.kotatsu.ui.download

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.PowerManager
import android.os.WorkSource
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import coil.Coil
import coil.api.get
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.inject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.local.MangaZip
import org.koitharu.kotatsu.ui.common.BaseService
import org.koitharu.kotatsu.ui.common.dialog.CheckBoxAlertDialog
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.retryUntilSuccess
import org.koitharu.kotatsu.utils.ext.safe
import org.koitharu.kotatsu.utils.ext.sub
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class DownloadService : BaseService() {

	private lateinit var notification: DownloadNotification
	private lateinit var wakeLock: PowerManager.WakeLock

	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()
	private val jobs = HashMap<Int, Job>()
	private val mutex = Mutex()

	override fun onCreate() {
		super.onCreate()
		notification = DownloadNotification(this)
		wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kotatsu:downloading")
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			ACTION_DOWNLOAD_START -> {
				val manga = intent.getParcelableExtra<Manga>(EXTRA_MANGA)
				val chapters = intent.getLongArrayExtra(EXTRA_CHAPTERS_IDS)?.toSet()
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
		return launch(Dispatchers.IO) {
			mutex.lock()
			wakeLock.acquire(TimeUnit.MINUTES.toMillis(20))
			withContext(Dispatchers.Main) {
				notification.fillFrom(manga)
				notification.setCancelId(startId)
				startForeground(DownloadNotification.NOTIFICATION_ID, notification())
			}
			val destination = getExternalFilesDir("manga")!!
			var output: MangaZip? = null
			try {
				val repo = MangaProviderFactory.create(manga.source)
				val cover = safe {
					Coil.loader().get(manga.coverUrl)
				}
				withContext(Dispatchers.Main) {
					notification.setLargeIcon(cover)
					notification.update()
				}
				val data = if (manga.chapters == null) repo.getDetails(manga) else manga
				output = MangaZip.findInDir(destination, data)
				output.prepare(data)
				val coverUrl = data.largeCoverUrl ?: data.coverUrl
				downloadPage(coverUrl, destination).let { file ->
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
							val url = repo.getPageFullUrl(page)
							val file = cache[url] ?: downloadPage(url, destination)
							output.addPage(
								chapter,
								file,
								pageIndex,
								MimeTypeMap.getFileExtensionFromUrl(url)
							)
							withContext(Dispatchers.Main) {
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
				}
				withContext(Dispatchers.Main) {
					notification.setCancelId(0)
					notification.setPostProcessing()
					notification.update()
				}
				output.compress()
				val result = MangaProviderFactory.createLocal().getFromFile(output.file)
				withContext(Dispatchers.Main) {
					notification.setDone(result)
					notification.dismiss()
					notification.update(manga.id.toInt().absoluteValue)
				}
			} catch (_: CancellationException) {
				withContext(Dispatchers.Main + NonCancellable) {
					notification.setCancelling()
					notification.setCancelId(0)
					notification.update()
				}
			} catch (e: Throwable) {
				withContext(Dispatchers.Main) {
					notification.setError(e)
					notification.setCancelId(0)
					notification.dismiss()
					notification.update(manga.id.toInt().absoluteValue)
				}
			} finally {
				withContext(NonCancellable) {
					jobs.remove(startId)
					output?.cleanup()
					destination.sub("page.tmp").delete()
					withContext(Dispatchers.Main) {
						stopForeground(true)
						notification.dismiss()
						stopSelf(startId)
					}
					wakeLock.release()
					mutex.unlock()
				}
			}
		}
	}

	private suspend fun downloadPage(url: String, destination: File): File {
		val request = Request.Builder()
			.url(url)
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
			val settings = AppSettings(context)
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