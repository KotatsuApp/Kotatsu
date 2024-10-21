package org.koitharu.kotatsu.reader.ui.pager

import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.Observer
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import java.io.IOException

class PageHolderDelegate(
	private val loader: PageLoader,
	private val readerSettings: ReaderSettings,
	private val callback: Callback,
	private val networkState: NetworkState,
	private val exceptionResolver: ExceptionResolver,
	private val isWebtoon: Boolean,
) : DefaultOnImageEventListener, Observer<ReaderSettings> {

	private val scope = loader.loaderScope + Dispatchers.Main.immediate
	var state = State.EMPTY
		private set
	private var job: Job? = null
	private var uri: Uri? = null
	private var cachedBounds: Rect? = null
	private var error: Throwable? = null

	init {
		scope.launch(Dispatchers.Main) { // the same as post() -- wait until child fields init
			callback.onConfigChanged()
		}
	}

	fun isLoading() = job?.isActive == true

	fun onBind(page: MangaPage) {
		val prevJob = job
		job = scope.launch {
			prevJob?.cancelAndJoin()
			doLoad(page, force = false)
		}
	}

	fun retry(page: MangaPage, isFromUser: Boolean) {
		val prevJob = job
		job = scope.launch {
			prevJob?.cancelAndJoin()
			val e = error
			if (e != null && ExceptionResolver.canResolve(e)) {
				if (!isFromUser) {
					return@launch
				}
				exceptionResolver.resolve(e)
			}
			doLoad(page, force = true)
		}
	}

	fun showErrorDetails(url: String?) {
		val e = error ?: return
		exceptionResolver.showDetails(e, url)
	}

	fun onAttachedToWindow() {
		readerSettings.observeForever(this)
	}

	fun onDetachedFromWindow() {
		readerSettings.removeObserver(this)
	}

	fun onRecycle() {
		state = State.EMPTY
		uri = null
		cachedBounds = null
		error = null
		job?.cancel()
	}

	fun reload() {
		if (state == State.SHOWN) {
			uri?.let {
				callback.onImageReady(it, cachedBounds)
			}
		}
	}

	override fun onReady() {
		state = State.SHOWING
		error = null
		callback.onImageShowing(readerSettings)
	}

	override fun onImageLoaded() {
		state = State.SHOWN
		error = null
		callback.onImageShown()
	}

	override fun onImageLoadError(e: Throwable) {
		e.printStackTraceDebug()
		val uri = this.uri
		error = e
		if (state == State.LOADED && e is IOException && uri != null && uri.toFileOrNull()?.exists() != false) {
			tryConvert(uri, e)
		} else {
			state = State.ERROR
			callback.onError(e)
		}
	}

	override fun onChanged(value: ReaderSettings) {
		if (state == State.SHOWN) {
			callback.onImageShowing(readerSettings)
		}
		callback.onConfigChanged()
	}

	private fun tryConvert(uri: Uri, e: Exception) {
		val prevJob = job
		job = scope.launch {
			prevJob?.join()
			state = State.CONVERTING
			try {
				val newUri = loader.convertBimap(uri)
				cachedBounds = if (readerSettings.isPagesCropEnabled(isWebtoon)) {
					loader.getTrimmedBounds(newUri)
				} else {
					null
				}
				state = State.CONVERTED
				callback.onImageReady(newUri, cachedBounds)
			} catch (ce: CancellationException) {
				throw ce
			} catch (e2: Throwable) {
				e2.printStackTrace()
				e.addSuppressed(e2)
				state = State.ERROR
				callback.onError(e)
			}
		}
	}

	private suspend fun doLoad(data: MangaPage, force: Boolean) {
		state = State.LOADING
		error = null
		callback.onLoadingStarted()
		yield()
		try {
			val task = withContext(Dispatchers.Default) {
				loader.loadPageAsync(data, force)
			}
			uri = coroutineScope {
				val progressObserver = observeProgress(this, task.progressAsFlow())
				val file = task.await()
				progressObserver.cancelAndJoin()
				file
			}
			state = State.LOADED
			cachedBounds = if (readerSettings.isPagesCropEnabled(isWebtoon)) {
				loader.getTrimmedBounds(checkNotNull(uri))
			} else {
				null
			}
			callback.onImageReady(checkNotNull(uri), cachedBounds)
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTraceDebug()
			state = State.ERROR
			error = e
			callback.onError(e)
			if (e is IOException && !networkState.value) {
				networkState.awaitForConnection()
				retry(data, isFromUser = false)
			}
		}
	}

	private fun observeProgress(scope: CoroutineScope, progress: Flow<Float>) = progress
		.debounce(250)
		.onEach { callback.onProgressChanged((100 * it).toInt()) }
		.launchIn(scope)

	enum class State {
		EMPTY, LOADING, LOADED, CONVERTING, CONVERTED, SHOWING, SHOWN, ERROR
	}

	interface Callback {

		fun onLoadingStarted()

		fun onError(e: Throwable)

		fun onImageReady(uri: Uri, bounds: Rect?)

		fun onImageShowing(settings: ReaderSettings)

		fun onImageShown()

		fun onProgressChanged(progress: Int)

		fun onConfigChanged()
	}
}
