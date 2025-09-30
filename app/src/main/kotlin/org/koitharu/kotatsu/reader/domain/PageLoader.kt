package org.koitharu.kotatsu.reader.domain

import android.content.Context`r`nimport android.graphics.Bitmap`r`nimport android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.CheckResult
import androidx.collection.LongSparseArray
import androidx.collection.set
import androidx.core.net.toFile
import androidx.core.net.toUri
import coil3.BitmapImage
import coil3.Image
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.toBitmap
import com.davemorrissey.labs.subscaleview.ImageSource
import dagger.hilt.android.ActivityRetainedLifecycle
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
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.LocalizedAppContext
import org.koitharu.kotatsu.core.image.BitmapDecoderCompat
import org.koitharu.kotatsu.core.network.CommonHeaders
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.network.imageproxy.ImageProxyInterceptor
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.MimeTypes
import org.koitharu.kotatsu.core.util.ext.URI_SCHEME_ZIP
import org.koitharu.kotatsu.core.util.ext.cancelChildrenAndJoin
import org.koitharu.kotatsu.core.util.ext.compressToPNG
import org.koitharu.kotatsu.core.util.ext.ensureRamAtLeast
import org.koitharu.kotatsu.core.util.ext.ensureSuccess
import org.koitharu.kotatsu.core.util.ext.getCompletionResultOrNull
import org.koitharu.kotatsu.core.util.ext.isFileUri
import org.koitharu.kotatsu.core.util.ext.isNotEmpty
import org.koitharu.kotatsu.core.util.ext.isPowerSaveMode
import org.koitharu.kotatsu.core.util.ext.isZipUri
import org.koitharu.kotatsu.core.util.ext.lifecycleScope
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra`r`nimport org.koitharu.kotatsu.reader.domain.panel.BitmapPanelImage`r`nimport org.koitharu.kotatsu.reader.domain.panel.PanelDetectionRequest`r`nimport org.koitharu.kotatsu.reader.domain.panel.Panel`r`nimport org.koitharu.kotatsu.reader.domain.panel.PanelDetectionResult`r`nimport org.koitharu.kotatsu.reader.domain.panel.PanelDetector`r`nimport org.koitharu.kotatsu.reader.domain.panel.PanelFlow
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.ramAvailable
import org.koitharu.kotatsu.core.util.ext.toMimeType
import org.koitharu.kotatsu.core.util.ext.use
import org.koitharu.kotatsu.core.util.ext.withProgress
import org.koitharu.kotatsu.core.util.progress.ProgressDeferred
import org.koitharu.kotatsu.download.ui.worker.DownloadSlowdownDispatcher
import org.koitharu.kotatsu.local.data.LocalStorageCache
import org.koitharu.kotatsu.local.data.PageCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.requireBody
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage`r`nimport kotlin.math.max
import java.io.File
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@ActivityRetainedScoped
class PageLoader @Inject constructor(
	@LocalizedAppContext private val context: Context,
	lifecycle: ActivityRetainedLifecycle,
	@MangaHttpClient private val okHttp: OkHttpClient,
	@PageCache private val cache: LocalStorageCache,
	private val coil: ImageLoader,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val imageProxyInterceptor: ImageProxyInterceptor,
	private val downloadSlowdownDispatcher: DownloadSlowdownDispatcher,`r`n	private val panelDetector: PanelDetector,`r`n) {

	val loaderScope = lifecycle.lifecycleScope + InternalErrorHandler() + Dispatchers.Default

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
	private val panelCache = LongSparseArray<PanelDetectionResult>()
	private val panelCacheLock = Any()

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

	suspend fun loadPreview(page: MangaPage): ImageSource? {
		val preview = page.preview
		if (preview.isNullOrEmpty()) {
			return null
		}
		val request = ImageRequest.Builder(context)
			.data(preview)
			.mangaSourceExtra(page.source)
			.transformations(TrimTransformation())
			.build()
		return coil.execute(request).image?.toImageSource()
	}

	fun peekPreviewSource(preview: String?): ImageSource? {
		if (preview.isNullOrEmpty()) {
			return null
		}
		coil.memoryCache?.let { cache ->
			val key = MemoryCache.Key(preview)
			cache[key]?.image?.let {
				return if (it is BitmapImage) {
					ImageSource.cachedBitmap(it.toBitmap())
				} else {
					ImageSource.bitmap(it.toBitmap())
				}
			}
		}
		coil.diskCache?.let { cache ->
			cache.openSnapshot(preview)?.use { snapshot ->
				return ImageSource.file(snapshot.data.toFile())
			}
		}
		return null
	}

	fun loadPageAsync(page: MangaPage, force: Boolean): ProgressDeferred<Uri, Float> {
		var task = tasks[page.id]?.takeIf { it.isValid() }
		if (force) {
			task?.cancel()
		} else if (task?.isCancelled == false) {
			return task
		}
		task = loadPageAsyncImpl(page, skipCache = force, isPrefetch = false)
		synchronized(tasks) {
			tasks[page.id] = task
		}
		return task
	}

	suspend fun loadPage(page: MangaPage, force: Boolean): Uri {
		return loadPageAsync(page, force).await()
	}

	@CheckResult
	suspend fun convertBimap(uri: Uri): Uri = convertLock.withLock {
		if (uri.isZipUri()) {
			runInterruptible(Dispatchers.IO) {
				ZipFile(uri.schemeSpecificPart).use { zip ->
					val entry = zip.getEntry(uri.fragment)
					context.ensureRamAtLeast(entry.size * 2)
					zip.getInputStream(entry).use {
						BitmapDecoderCompat.decode(it, MimeTypes.getMimeTypeFromExtension(entry.name))
					}
				}
			}.use { image ->
				cache.set(uri.toString(), image).toUri()
			}
		} else {
			val file = uri.toFile()
			runInterruptible(Dispatchers.IO) {
				context.ensureRamAtLeast(file.length() * 2)
				BitmapDecoderCompat.decode(file)
			}.use { image ->
				image.compressToPNG(file)
			}
			uri
		}
	}

	suspend fun getTrimmedBounds(uri: Uri): Rect? = runCatchingCancellable {
		edgeDetector.getBounds(ImageSource.uri(uri))
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
				synchronized(tasks) {
					tasks[page.id] = loadPageAsyncImpl(page, skipCache = false, isPrefetch = true)
				}
			}
		}
	}

	private fun loadPageAsyncImpl(
		page: MangaPage,
		skipCache: Boolean,
		isPrefetch: Boolean,
	): ProgressDeferred<Uri, Float> {
		synchronized(panelCacheLock) {
			panelCache.remove(page.id)
		}

		val progress = MutableStateFlow(PROGRESS_UNDEFINED)
		val deferred = loaderScope.async {
			counter.incrementAndGet()
			try {
				loadPageImpl(
					page = page,
					progress = progress,
					isPrefetch = isPrefetch,
					skipCache = skipCache,
				)
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

	private suspend fun loadPageImpl(
		page: MangaPage,
		progress: MutableStateFlow<Float>,
		isPrefetch: Boolean,
		skipCache: Boolean,
	): Uri = semaphore.withPermit {
		val pageUrl = getPageUrl(page)
		check(pageUrl.isNotBlank()) { "Cannot obtain full image url for $page" }
		if (!skipCache) {
			cache.get(pageUrl)?.let { return it.toUri() }
		}
		val uri = pageUrl.toUri()
		return when {
			uri.isZipUri() -> if (uri.scheme == URI_SCHEME_ZIP) {
				uri
			} else { // legacy uri
				uri.buildUpon().scheme(URI_SCHEME_ZIP).build()
			}

			uri.isFileUri() -> uri
			else -> {
				if (isPrefetch) {
					downloadSlowdownDispatcher.delay(page.source)
				}
				val request = createPageRequest(pageUrl, page.source)
				imageProxyInterceptor.interceptPageRequest(request, okHttp).ensureSuccess().use { response ->
					response.requireBody().withProgress(progress).use {
						cache.set(pageUrl, it.source(), it.contentType()?.toMimeType())
					}
				}.toUri()
			}
		}
	}

	private fun isLowRam(): Boolean {
		return context.ramAvailable <= FileSize.MEGABYTES.convert(PREFETCH_MIN_RAM_MB, FileSize.BYTES)
	}

	private fun Image.toImageSource(): ImageSource = if (this is BitmapImage) {
		ImageSource.cachedBitmap(toBitmap())
	} else {
		ImageSource.bitmap(toBitmap())
	}

	private fun Deferred<Uri>.isValid(): Boolean {
		return getCompletionResultOrNull()?.map { uri ->
			uri.exists() && uri.isTargetNotEmpty()
		}?.getOrDefault(false) != false
	}

	private class InternalErrorHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
		CoroutineExceptionHandler {

		override fun handleException(context: CoroutineContext, exception: Throwable) {
			exception.printStackTraceDebug()
		}
	}

	
	suspend fun detectPanels(
		page: MangaPage,
		pageIndex: Int,
		uri: Uri,
		flow: PanelFlow,
		isDoublePage: Boolean,
		maxPanels: Int = PanelDetectionRequest.DEFAULT_MAX_PANELS,
		minPanelAreaRatio: Float = PanelDetectionRequest.DEFAULT_MIN_PANEL_AREA_RATIO,
	): PanelDetectionResult? {
		val cached = synchronized(panelCacheLock) {
			panelCache[page.id]
		}
		val baseResult = if (cached != null) {
			cached
		} else {
			val bitmap = decodePanelBitmap(uri) ?: return null
			try {
				val request = PanelDetectionRequest(
					image = BitmapPanelImage(bitmap),
					pageIndex = pageIndex,
					isDoublePage = isDoublePage,
					preferredFlow = PanelFlow.LeftToRight,
					maxPanels = maxPanels,
					minPanelAreaRatio = minPanelAreaRatio,
				)
				runCatching { panelDetector.detect(request) }.getOrNull()?.also { result ->
					synchronized(panelCacheLock) {
						panelCache[page.id] = result
					}
				}
			} finally {
				bitmap.recycle()
			}
		}
		return baseResult?.let { reorderPanels(it, flow) }
	}

	private fun reorderPanels(result: PanelDetectionResult, flow: PanelFlow): PanelDetectionResult {
		if (result.primary.flow == flow) {
			return result
		}
		val comparator = panelComparator(flow)
		val primary = result.primary.copy(
			panels = result.primary.panels.sortedWith(comparator),
			flow = flow,
		)
		val alternatives = result.alternatives.map { sequence ->
			if (sequence.flow == flow) {
				sequence
			} else {
				sequence.copy(
					panels = sequence.panels.sortedWith(comparator),
					flow = flow,
				)
			}
		}
		return result.copy(primary = primary, alternatives = alternatives)
	}

	private fun panelComparator(flow: PanelFlow): Comparator<Panel> = when (flow) {
		PanelFlow.LeftToRight -> Comparator { a, b ->
			val row = a.bounds.top.compareTo(b.bounds.top)
			if (row != 0) row else a.bounds.left.compareTo(b.bounds.left)
		}
		PanelFlow.RightToLeft -> Comparator { a, b ->
			val row = a.bounds.top.compareTo(b.bounds.top)
			if (row != 0) row else b.bounds.left.compareTo(a.bounds.left)
		}
		PanelFlow.TopToBottom -> Comparator { a, b ->
			val column = a.bounds.left.compareTo(b.bounds.left)
			if (column != 0) column else a.bounds.top.compareTo(b.bounds.top)
		}
	}

	private suspend fun decodePanelBitmap(uri: Uri, maxDimension: Int = PANEL_MAX_DIMENSION): Bitmap? = runInterruptible(Dispatchers.IO) {
		when {
			uri.isFileUri() -> decodeFileSampled(uri.toFile(), maxDimension)
			uri.isZipUri() -> decodeZipSampled(uri, maxDimension)
			else -> null
		}
	}

	private fun decodeFileSampled(file: File, maxDimension: Int): Bitmap? {
		val options = BitmapFactory.Options().apply {
			inJustDecodeBounds = true
		}
		BitmapFactory.decodeFile(file.absolutePath, options)
		if (options.outWidth <= 0 || options.outHeight <= 0) {
			return null
		}
		options.inSampleSize = calculateInSampleSize(options, maxDimension)
		options.inJustDecodeBounds = false
		options.inPreferredConfig = Bitmap.Config.RGB_565
		return BitmapFactory.decodeFile(file.absolutePath, options)
	}

	private fun decodeZipSampled(uri: Uri, maxDimension: Int): Bitmap? {
		val archivePath = uri.schemeSpecificPart ?: return null
		val entryName = uri.fragment ?: return null
		val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		ZipFile(archivePath).use { zip ->
			val entry = zip.getEntry(entryName) ?: return null
			zip.getInputStream(entry).use { stream ->
				BitmapFactory.decodeStream(stream, null, boundsOptions)
			}
			if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
				return null
			}
			val decodeOptions = BitmapFactory.Options().apply {
				inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
				inPreferredConfig = Bitmap.Config.RGB_565
			}
			zip.getInputStream(entry).use { stream ->
				return BitmapFactory.decodeStream(stream, null, decodeOptions)
			}
		}
	}

	private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
		var inSampleSize = 1
		val width = options.outWidth
		val height = options.outHeight
		if (height <= 0 || width <= 0) {
			return inSampleSize
		}
		while (max(height / inSampleSize, width / inSampleSize) > maxDimension) {
			inSampleSize *= 2
		}
		return inSampleSize
	}
companion object {

		private const val PROGRESS_UNDEFINED = -1f
		private const val PREFETCH_LIMIT_DEFAULT = 6
		private const val PANEL_MAX_DIMENSION = 1600
		private const val PREFETCH_MIN_RAM_MB = 80L

		fun createPageRequest(pageUrl: String, mangaSource: MangaSource) = Request.Builder()
			.url(pageUrl)
			.get()
			.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
			.cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
			.tag(MangaSource::class.java, mangaSource)
			.build()


		@Blocking
		private fun Uri.exists(): Boolean = when {
			isFileUri() -> toFile().exists()
			isZipUri() -> {
				val file = File(requireNotNull(schemeSpecificPart))
				file.exists() && ZipFile(file).use { it.getEntry(fragment) != null }
			}

			else -> false
		}

		@Blocking
		private fun Uri.isTargetNotEmpty(): Boolean = when {
			isFileUri() -> toFile().isNotEmpty()
			isZipUri() -> {
				val file = File(requireNotNull(schemeSpecificPart))
				file.exists() && ZipFile(file).use { (it.getEntry(fragment)?.size ?: 0L) != 0L }
			}

			else -> false
		}
	}
}










