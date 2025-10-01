package org.koitharu.kotatsu.reader.ui.pager.vm

import android.graphics.Rect
import android.net.Uri
import androidx.annotation.WorkerThread
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import com.davemorrissey.labs.subscaleview.ImageSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow`r`nimport kotlinx.coroutines.flow.StateFlow`r`nimport kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import okio.IOException
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.throttle
import org.koitharu.kotatsu.reader.domain.panel.PanelDetectionResult`r`nimport org.koitharu.kotatsu.reader.domain.panel.PanelFlow`r`nimport org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings

class PageViewModel(
	private val loader: PageLoader,
	val settingsProducer: ReaderSettings.Producer,
	private val networkState: NetworkState,
	private val exceptionResolver: ExceptionResolver,
	private val isWebtoon: Boolean,
) : DefaultOnImageEventListener {

		private val scope = loader.loaderScope + Dispatchers.Main.immediate
	private var job: Job? = null
	private var panelJob: Job? = null
	private var cachedBounds: Rect? = null
	private var currentPage: ReaderPage? = null
	private var lastUri: Uri? = null

	private val _panelState = MutableStateFlow<PanelDetectionResult?>(null)
	val panelState: StateFlow<PanelDetectionResult?> = _panelState.asStateFlow()

	val state = MutableStateFlow<PageState>(PageState.Empty)

	fun isLoading() = job?.isActive == true

	fun onBind(page: ReaderPage) {
		currentPage = page
		lastUri = null
		_panelState.value = null
		val prevJob = job
		job = scope.launch(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			doLoad(page, force = false)
		}
	}

	fun retry(page: ReaderPage, isFromUser: Boolean) {
		val prevJob = job
		job = scope.launch {
			prevJob?.cancelAndJoin()
			val e = (state.value as? PageState.Error)?.error
			if (e != null && ExceptionResolver.canResolve(e)) {
				if (isFromUser) {
					exceptionResolver.resolve(e)
				}
			}
			withContext(Dispatchers.Default) {
				doLoad(page, force = true)
			}
		}
	}

	fun showErrorDetails(url: String?) {
		val e = (state.value as? PageState.Error)?.error ?: return
		exceptionResolver.showErrorDetails(e, url)
	}

	fun onRecycle() {
		state.value = PageState.Empty
		cachedBounds = null
		job?.cancel()
	}

	override fun onImageLoaded() {
		state.update { currentState ->
			if (currentState is PageState.Loaded) {
				PageState.Shown(currentState.source, currentState.isConverted)
			} else {
				currentState
			}
		}
	}

	override fun onImageLoadError(e: Throwable) {
		e.printStackTraceDebug()

		state.update { currentState ->
			if (currentState is PageState.Loaded) {
				val uri = (currentState.source as? ImageSource.Uri)?.uri
				if (!currentState.isConverted && uri != null && e is IOException) {
					tryConvert(uri, e)
					PageState.Converting()
				} else {
					PageState.Error(e)
				}
			} else {
				currentState
			}
		}
	}

	private fun tryConvert(uri: Uri, e: Exception) {
		val prevJob = job
		job = scope.launch(Dispatchers.Default) {
			prevJob?.join()
			state.value = PageState.Converting()
			try {
				val newUri = loader.convertBimap(uri)
				cachedBounds = if (settingsProducer.value.isPagesCropEnabled(isWebtoon)) {
					loader.getTrimmedBounds(newUri)
				} else {
					null
				}
				state.value = PageState.Loaded(newUri.toImageSource(cachedBounds), isConverted = true)
				lastUri = newUri
				currentPage?.let { launchPanelDetection(it, newUri) }
			} catch (ce: CancellationException) {
				throw ce
			} catch (e2: Throwable) {
				e2.printStackTrace()
				e.addSuppressed(e2)
				state.value = PageState.Error(e)
			}
		}
	}

	@WorkerThread
		private suspend fun doLoad(page: ReaderPage, force: Boolean) = coroutineScope {
		state.value = PageState.Loading(null, -1)
		val mangaPage = page.toMangaPage()
		val previewJob = launch {
			val preview = loader.loadPreview(mangaPage) ?: return@launch
			state.update {
				if (it is PageState.Loading) it.copy(preview = preview) else it
			}
		}
		try {
			val task = loader.loadPageAsync(mangaPage, force)
			val progressObserver = observeProgress(this, task.progressAsFlow())
			val uri = task.await()
			progressObserver.cancelAndJoin()
			previewJob.cancel()
			cachedBounds = if (settingsProducer.value.isPagesCropEnabled(isWebtoon)) {
				loader.getTrimmedBounds(uri)
			} else {
				null
			}
			lastUri = uri
			state.value = PageState.Loaded(uri.toImageSource(cachedBounds), isConverted = false)
			launchPanelDetection(page, uri)
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTraceDebug()
			state.value = PageState.Error(e)
			if (e is IOException && !networkState.value) {
				networkState.awaitForConnection()
				retry(page, isFromUser = false)
			}
		}
	}
	fun requestPanelDetection() {
		val page = currentPage ?: return
		val uri = lastUri ?: return
		launchPanelDetection(page, uri)
	}

	private fun launchPanelDetection(page: ReaderPage, uri: Uri) {
		val settings = settingsProducer.value
		if (!settings.isPanelViewEnabled) {
			_panelState.value = null
			return
		}
		panelJob?.cancel()
		_panelState.value = null
		panelJob = scope.launch(Dispatchers.Default) {
			val result = loader.detectPanels(
				page = page.toMangaPage(),
				pageIndex = page.index,
				uri = uri,
				flow = settings.panelPreferences.readingOrder.toPanelFlow(),
				isDoublePage = settings.isDoublePagesOnLandscape,
			)
			_panelState.value = result
		}
	}

	private fun observeProgress(scope: CoroutineScope, progress: Flow<Float>) = progress
		.throttle(250)
		.onEach {
			val progressValue = (100 * it).toInt()
			state.update { currentState ->
				if (currentState is PageState.Loading) {
					currentState.copy(progress = progressValue)
				} else {
					currentState
				}
			}
		}.launchIn(scope)

	private fun Uri.toImageSource(bounds: Rect?): ImageSource {
		val source = ImageSource.uri(this)
		return if (bounds != null) {
			source.region(bounds)
		} else {
			source
		}
	}
}














