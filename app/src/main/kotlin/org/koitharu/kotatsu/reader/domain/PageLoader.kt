package org.koitharu.kotatsu.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.collection.LongSparseArray
import androidx.collection.set
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.ImageProxyInterceptor
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.RetainedLifecycleCoroutineScope
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.core.util.ext.getCompletionResultOrNull
import org.koitharu.kotatsu.core.util.ext.isNotEmpty
import org.koitharu.kotatsu.core.util.ext.isPowerSaveMode
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.ramAvailable
import org.koitharu.kotatsu.core.util.ext.withProgress
import org.koitharu.kotatsu.core.util.progress.ProgressDeferred
import org.koitharu.kotatsu.core.zip.ZipPool
import org.koitharu.kotatsu.local.data.CbzFilter
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import java.io.File
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@ActivityRetainedScoped
class PageLoader @Inject constructor(
	@ApplicationContext private val context: Context,
	lifecycle: ActivityRetainedLifecycle,
	@MangaHttpClient private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val imageProxyInterceptor: ImageProxyInterceptor,
) : RetainedLifecycle.OnClearedListener {

	init {
		lifecycle.addOnClearedListener(this)
	}

	val loaderScope = RetainedLifecycleCoroutineScope(lifecycle) + InternalErrorHandler() + Dispatchers.Default

	private val tasks = LongSparseArray<ProgressDeferred<File, Float>>()
	private val semaphore = Semaphore(3)
	private val convertLock = Mutex()
	private val prefetchLock = Mutex()
	private var repository: MangaRepository? = null
	private val prefetchQueue = LinkedList<MangaPage>()
	private val zipPool = ZipPool(2)
	private val counter = AtomicInteger(0)
	private var prefetchQueueLimit = PREFETCH_LIMIT_DEFAULT // TODO adaptive

	override fun onCleared() {
		synchronized(tasks) {
			tasks.clear()
		}
		zipPool.evictAll()
	}

	fun isPrefetchApplicable(): Boolean {
		return repository is RemoteMangaRepository
			&& settings.isPagesPreloadEnabled
			&& !context.isPowerSaveMode()
			&& !isLowRam()
	}

	@AnyThread
	fun prefetch(pages: List<ReaderPage>) = loaderScope.launch {
		prefetchLock.withLock {
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
		var task = tasks[page.id]?.takeIf { it.isValid() }
		if (force) {
			task?.cancel()
		} else if (task?.isCancelled == false) {
			return task
		}
		task = loadPageAsyncImpl(page, force)
		synchronized(tasks) {
			tasks[page.id] = task
		}
		return task
	}

	suspend fun loadPage(page: MangaPage, force: Boolean): File {
		return loadPageAsync(page, force).await()
	}

	suspend fun convertInPlace(file: File) {
		convertLock.withLock {
			if (context.ramAvailable < file.length() * 2) {
				return@withLock
			}
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

	suspend fun getPageUrl(page: MangaPage): String {
		return getRepository(page.source).getPageUrl(page)
	}

	private fun onIdle() = loaderScope.launch {
		prefetchLock.withLock {
			while (prefetchQueue.isNotEmpty()) {
				val page = prefetchQueue.pollFirst() ?: return@launch
				if (cache.get(page.url) == null) {
					synchronized(tasks) {
						tasks[page.id] = loadPageAsyncImpl(page, false)
					}
					return@launch
				}
			}
		}
	}

	private fun loadPageAsyncImpl(page: MangaPage, skipCache: Boolean): ProgressDeferred<File, Float> {
		val progress = MutableStateFlow(PROGRESS_UNDEFINED)
		val deferred = loaderScope.async {
			if (!skipCache) {
				cache.get(page.url)?.let { return@async it }
			}
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
			mangaRepositoryFactory.create(source).also { repository = it }
		}
	}

	private suspend fun loadPageImpl(page: MangaPage, progress: MutableStateFlow<Float>): File = semaphore.withPermit {
		val pageUrl = getPageUrl(page)
		check(pageUrl.isNotBlank()) { "Cannot obtain full image url" }
		val uri = Uri.parse(pageUrl)
		return if (CbzFilter.isUriSupported(uri)) {
			runInterruptible(Dispatchers.IO) {
				zipPool[uri]
			}.use {
				cache.put(pageUrl, it)
			}
		} else {
			val request = createPageRequest(page, pageUrl)
			imageProxyInterceptor.interceptPageRequest(request, okHttp).ensureSuccess().use { response ->
				val body = checkNotNull(response.body) {
					"Null response"
				}
				body.withProgress(progress).use {
					cache.put(pageUrl, it.source())
				}
			}
		}
	}

	private fun isLowRam(): Boolean {
		return context.ramAvailable <= FileSize.MEGABYTES.convert(PREFETCH_MIN_RAM_MB, FileSize.BYTES)
	}

	private fun Deferred<File>.isValid(): Boolean {
		return getCompletionResultOrNull()?.map { file ->
			file.exists() && file.isNotEmpty()
		}?.getOrDefault(false) ?: true
	}

	private class InternalErrorHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
		CoroutineExceptionHandler {

		override fun handleException(context: CoroutineContext, exception: Throwable) {
			exception.printStackTraceDebug()
		}
	}

	companion object {

		private const val PROGRESS_UNDEFINED = -1f
		private const val PREFETCH_LIMIT_DEFAULT = 10
		private const val PREFETCH_MIN_RAM_MB = 80L

		fun createPageRequest(page: MangaPage, pageUrl: String) = Request.Builder()
			.url(pageUrl)
			.get()
			.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
			.cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
			.tag(MangaSource::class.java, page.source)
			.build()
	}
}
