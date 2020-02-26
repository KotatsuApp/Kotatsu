package org.koitharu.kotatsu.ui.download

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import coil.Coil
import coil.api.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.inject
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.local.MangaZip
import org.koitharu.kotatsu.ui.common.BaseService
import org.koitharu.kotatsu.utils.CacheUtils
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.retryUntilSuccess
import org.koitharu.kotatsu.utils.ext.safe
import org.koitharu.kotatsu.utils.ext.sub
import java.io.File
import kotlin.math.absoluteValue

class DownloadService : BaseService() {

	private lateinit var notification: DownloadNotification

	private val okHttp by inject<OkHttpClient>()
	private val cache by inject<PagesCache>()

	override fun onCreate() {
		super.onCreate()
		notification = DownloadNotification(this)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val manga = intent?.getParcelableExtra<Manga>(EXTRA_MANGA)
		val chapters = intent?.getLongArrayExtra(EXTRA_CHAPTERS_IDS)?.toSet()
		if (manga != null) {
			downloadManga(manga, chapters)
		} else {
			stopSelf(startId)
		}
		return START_NOT_STICKY
	}

	private fun downloadManga(manga: Manga, chaptersIds: Set<Long>?) {
		val destination = getExternalFilesDir("manga")!!
		notification.fillFrom(manga)
		startForeground(DownloadNotification.NOTIFICATION_ID, notification())
		launch(Dispatchers.IO) {
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
					notification.setPostProcessing()
					notification.update()
				}
				output.compress()
				withContext(Dispatchers.Main) {
					notification.setDone()
					notification.update(manga.id.toInt().absoluteValue)
				}
			} finally {
				withContext(NonCancellable) {
					output?.cleanup()
					destination.sub("page.tmp").delete()
					withContext(Dispatchers.Main) {
						stopForeground(true)
					}
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

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTERS_IDS = "chapters_ids"

		fun start(context: Context, manga: Manga, chaptersIds: Collection<Long>? = null) {
			val intent = Intent(context, DownloadService::class.java)
			intent.putExtra(EXTRA_MANGA, manga)
			if (chaptersIds != null) {
				intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toLongArray())
			}
			ContextCompat.startForegroundService(context, intent)
		}
	}
}