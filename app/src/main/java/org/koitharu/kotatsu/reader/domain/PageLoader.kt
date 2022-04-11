package org.koitharu.kotatsu.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.collection.set
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Closeable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.connectivityManager
import org.koitharu.kotatsu.utils.progress.ProgressDeferred

private const val PROGRESS_UNDEFINED = -1f
private const val PREFETCH_LIMIT_DEFAULT = 10

class PageLoader : KoinComponent, Closeable {

	val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private val okHttp = get<OkHttpClient>()
	private val cache = get<PagesCache>()
	private val settings = get<AppSettings>()
	private val connectivityManager = get<Context>().connectivityManager
	private val tasks = LongSparseArray<ProgressDeferred<File, Float>>()
	private val convertLock = Mutex()
	private var repository: MangaRepository? = null
	private var prefetchQueue = LinkedList<MangaPage>()
	private val counter = AtomicInteger(0)
	private var prefetchQueueLimit = PREFETCH_LIMIT_DEFAULT // TODO adaptive
	private val emptyProgressFlow: StateFlow<Float> = MutableStateFlow(-1f)

	override fun close() {
		loaderScope.cancel()
		tasks.clear()
	}

	fun isPrefetchApplicable(): Boolean {
		return repository is RemoteMangaRepository && settings.isPagesPreloadAllowed(connectivityManager)
	}

	fun prefetch(pages: List<ReaderPage>) {
		synchronized(prefetchQueue) {
			for (page in pages.asReversed()) {
				if (tasks.containsKey(page.id)) {
					continue
				}
				prefetchQueue.offerFirst(page.toMangaPage())
				if (prefetchQueue.size > prefetchQueueLimit) {
					prefetchQueue.pollLast()
				}
			}
		}
		if (counter.get() == 0) {
			onIdle()
		}
	}

	fun loadPageAsync(page: MangaPage, force: Boolean): ProgressDeferred<File, Float> {
		if (!force) {
			cache[page.url]?.let {
				return getCompletedTask(it)
			}
		}
		var task = tasks[page.id]
		if (force) {
			task?.cancel()
		} else if (task?.isCancelled == false) {
			return task
		}
		task = loadPageAsyncImpl(page)
		tasks[page.id] = task
		return task
	}

	suspend fun loadPage(page: MangaPage, force: Boolean): File {
		return loadPageAsync(page, force).await()
	}

	suspend fun convertInPlace(file: File) {
		convertLock.withLock {
			runInterruptible(Dispatchers.Default) {
				val image = BitmapFactory.decodeFile(file.absolutePath)
				try {
					file.outputStream().use { out ->
						image.compress(Bitmap.CompressFormat.PNG, 100, out)
					}
				} finally {
					image.recycle()
				}
			}
		}
	}

	private fun onIdle() {
		synchronized(prefetchQueue) {
			val page = prefetchQueue.pollFirst() ?: return
			tasks[page.id] = loadPageAsyncImpl(page)
		}
	}

	private fun loadPageAsyncImpl(page: MangaPage): ProgressDeferred<File, Float> {
		val progress = MutableStateFlow(PROGRESS_UNDEFINED)
		val deferred = loaderScope.async {
			counter.incrementAndGet()
			try {
				loadPageImpl(page, progress)
			} finally {
				if (counter.decrementAndGet() == 0) {
					onIdle()
				}
			}
		}
		return ProgressDeferred(deferred, progress)
	}

	@Synchronized
	private fun getRepository(source: MangaSource): MangaRepository {
		val result = repository
		return if (result != null && result.source == source) {
			result
		} else {
			MangaRepository(source).also { repository = it }
		}
	}

	private suspend fun loadPageImpl(page: MangaPage, progress: MutableStateFlow<Float>): File {
		val pageUrl = getRepository(page.source).getPageUrl(page)
		check(pageUrl.isNotBlank()) { "Cannot obtain full image url" }
		val uri = Uri.parse(pageUrl)
		return if (uri.scheme == "cbz") {
			runInterruptible(Dispatchers.IO) {
				val zip = ZipFile(uri.schemeSpecificPart)
				val entry = zip.getEntry(uri.fragment)
				zip.getInputStream(entry).use {
					cache.put(pageUrl, it)
				}
			}
		} else {
			val request = Request.Builder()
				.url(pageUrl)
				.get()
				.header(CommonHeaders.REFERER, page.referer)
				.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
				.cacheControl(CommonHeaders.CACHE_CONTROL_DISABLED)
				.build()
			okHttp.newCall(request).await().use { response ->
				check(response.isSuccessful) {
					"Invalid response: ${response.code} ${response.message}"
				}
				val body = checkNotNull(response.body) {
					"Null response"
				}
				runInterruptible(Dispatchers.IO) {
					body.byteStream().use {
						cache.put(pageUrl, it, body.contentLength(), progress)
					}
				}
			}
		}
	}

	private fun getCompletedTask(file: File): ProgressDeferred<File, Float> {
		val deferred = CompletableDeferred(file)
		return ProgressDeferred(deferred, emptyProgressFlow)
	}
}