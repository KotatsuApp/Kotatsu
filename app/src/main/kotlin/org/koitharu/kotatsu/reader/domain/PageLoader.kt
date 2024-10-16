package org.koitharu.kotatsu.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.collection.LongSparseArray
import androidx.collection.set
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import dagger.hilt.android.ActivityRetainedLifecycle
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
import okio.use
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.network.imageproxy.ImageProxyInterceptor
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.RetainedLifecycleCoroutineScope
import org.koitharu.kotatsu.core.util.ext.URI_SCHEME_ZIP
import org.koitharu.kotatsu.core.util.ext.cancelChildrenAndJoin
import org.koitharu.kotatsu.core.util.ext.compressToPNG
import org.koitharu.kotatsu.core.util.ext.ensureRamAtLeast
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.core.util.ext.exists
import org.koitharu.kotatsu.core.util.ext.getCompletionResultOrNull
import org.koitharu.kotatsu.core.util.ext.isPowerSaveMode
import org.koitharu.kotatsu.core.util.ext.isTargetNotEmpty
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.ramAvailable
import org.koitharu.kotatsu.core.util.ext.use
import org.koitharu.kotatsu.core.util.ext.withProgress
import org.koitharu.kotatsu.core.util.progress.ProgressDeferred
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.local.data.isFileUri
import org.koitharu.kotatsu.local.data.isZipUri
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.requireBody
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.concurrent.Volatile
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
) {

	val loaderScope = RetainedLifecycleCoroutineScope(lifecycle) + InternalErrorHandler() + Dispatchers.Default

	private val tasks = LongSparseArray<ProgressDeferred<Uri, Float>>()
	private val semaphore = Semaphore(3)
	private val convertLock = Mutex()
	private val prefetchLock = Mutex()

	@Volatile
	private var repository: MangaRepository? = null
	private val prefetchQueue = LinkedList<MangaPage>()
	private val counter = AtomicInteger(0)
	private var prefetchQueueLimit = PREFETCH_LIMIT_DEFAULT // TODO adaptive
	private val edgeDetector = EdgeDetector(context)

	fun isPrefetchApplicable(): Boolean {
		return repository is CachingMangaRepository
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

	fun loadPageAsync(page: MangaPage, force: Boolean): ProgressDeferred<Uri, Float> {
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

	suspend fun loadPage(page: MangaPage, force: Boolean): Uri {
		return loadPageAsync(page, force).await()
	}

	suspend fun convertBimap(uri: Uri): Uri = convertLock.withLock {
		if (uri.isZipUri()) {
			val bitmap = runInterruptible(Dispatchers.IO) {
				ZipFile(uri.schemeSpecificPart).use { zip ->
					val entry = zip.getEntry(uri.fragment)
					context.ensureRamAtLeast(entry.size * 2)
					zip.getInputStream(zip.getEntry(uri.fragment)).use {
						checkBitmapNotNull(BitmapFactory.decodeStream(it))
					}
				}
			}
			cache.put(uri.toString(), bitmap).toUri()
		} else {
			val file = uri.toFile()
			context.ensureRamAtLeast(file.length() * 2)
			runInterruptible(Dispatchers.IO) {
				checkBitmapNotNull(BitmapFactory.decodeFile(file.absolutePath))
			}.use { image ->
				image.compressToPNG(file)
			}
			uri
		}
	}

	suspend fun getTrimmedBounds(uri: Uri): Rect? = runCatchingCancellable {
		edgeDetector.getBounds(ImageSource.Uri(uri))
	}.onFailure { error ->
		error.printStackTraceDebug()
	}.getOrNull()

	suspend fun getPageUrl(page: MangaPage): String {
		return getRepository(page.source).getPageUrl(page)
	}

	suspend fun invalidate(clearCache: Boolean) {
		tasks.clear()
		loaderScope.cancelChildrenAndJoin()
		if (clearCache) {
			cache.clear()
		}
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

	private fun loadPageAsyncImpl(page: MangaPage, skipCache: Boolean): ProgressDeferred<Uri, Float> {
		val progress = MutableStateFlow(PROGRESS_UNDEFINED)
		val deferred = loaderScope.async {
			if (!skipCache) {
				cache.get(page.url)?.let { return@async it.toUri() }
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

	private suspend fun loadPageImpl(page: MangaPage, progress: MutableStateFlow<Float>): Uri = semaphore.withPermit {
		val pageUrl = getPageUrl(page)
		check(pageUrl.isNotBlank()) { "Cannot obtain full image url for $page" }
		val uri = Uri.parse(pageUrl)
		return when {
			uri.isZipUri() -> if (uri.scheme == URI_SCHEME_ZIP) {
				uri
			} else { // legacy uri
				uri.buildUpon().scheme(URI_SCHEME_ZIP).build()
			}

			uri.isFileUri() -> uri
			else -> {
				val request = createPageRequest(pageUrl, page.source)
				imageProxyInterceptor.interceptPageRequest(request, okHttp).ensureSuccess().use { response ->
					response.requireBody().withProgress(progress).use {
						cache.put(pageUrl, it.source())
					}
				}.toUri()
			}
		}
	}

	private fun isLowRam(): Boolean {
		return context.ramAvailable <= FileSize.MEGABYTES.convert(PREFETCH_MIN_RAM_MB, FileSize.BYTES)
	}

	private fun checkBitmapNotNull(bitmap: Bitmap?): Bitmap = checkNotNull(bitmap) { "Cannot decode bitmap" }

	private fun Deferred<Uri>.isValid(): Boolean {
		return getCompletionResultOrNull()?.map { uri ->
			uri.exists() && uri.isTargetNotEmpty()
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
		private const val PREFETCH_LIMIT_DEFAULT = 6
		private const val PREFETCH_MIN_RAM_MB = 80L

		fun createPageRequest(pageUrl: String, mangaSource: MangaSource) = Request.Builder()
			.url(pageUrl)
			.get()
			.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
			.cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
			.tag(MangaSource::class.java, mangaSource)
			.build()
	}
}
