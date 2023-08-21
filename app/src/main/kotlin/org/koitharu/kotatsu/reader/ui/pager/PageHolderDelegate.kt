package org.koitharu.kotatsu.reader.ui.pager

import android.net.Uri
import androidx.core.net.toUri
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
import kotlinx.coroutines.yield
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import java.io.File
import java.io.IOException

class PageHolderDelegate(
	private val loader: PageLoader,
	private val readerSettings: ReaderSettings,
	private val callback: Callback,
	private val networkState: NetworkState,
	private val exceptionResolver: ExceptionResolver,
) : DefaultOnImageEventListener, Observer<ReaderSettings> {

	private val scope = loader.loaderScope + Dispatchers.Main.immediate
	private var state = State.EMPTY
	private var job: Job? = null
	private var file: File? = null
	private var error: Throwable? = null

	init {
		callback.onConfigChanged()
	}

	fun onBind(page: MangaPage) {
		val prevJob = job
		job = scope.launch {
			prevJob?.cancelAndJoin()
			doLoad(page, force = false)
		}
	}

	fun retry(page: MangaPage) {
		val prevJob = job
		job = scope.launch {
			prevJob?.cancelAndJoin()
			val e = error
			if (e != null && ExceptionResolver.canResolve(e)) {
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
		file = null
		error = null
		job?.cancel()
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
		val file = this.file
		error = e
		if (state == State.LOADED && e is IOException && file != null && file.exists()) {
			tryConvert(file, e)
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

	private fun tryConvert(file: File, e: Exception) {
		val prevJob = job
		job = scope.launch {
			prevJob?.join()
			state = State.CONVERTING
			try {
				loader.convertInPlace(file)
				state = State.CONVERTED
				callback.onImageReady(file.toUri())
			} catch (ce: CancellationException) {
				throw ce
			} catch (e2: Throwable) {
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
			val task = loader.loadPageAsync(data, force)
			file = coroutineScope {
				val progressObserver = observeProgress(this, task.progressAsFlow())
				val file = task.await()
				progressObserver.cancelAndJoin()
				file
			}
			state = State.LOADED
			callback.onImageReady(checkNotNull(file).toUri())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTraceDebug()
			state = State.ERROR
			error = e
			callback.onError(e)
			if (e is IOException && !networkState.value) {
				networkState.awaitForConnection()
				retry(data)
			}
		}
	}

	private fun observeProgress(scope: CoroutineScope, progress: Flow<Float>) = progress
		.debounce(250)
		.onEach { callback.onProgressChanged((100 * it).toInt()) }
		.launchIn(scope)

	private enum class State {
		EMPTY, LOADING, LOADED, CONVERTING, CONVERTED, SHOWING, SHOWN, ERROR
	}

	interface Callback {

		fun onLoadingStarted()

		fun onError(e: Throwable)

		fun onImageReady(uri: Uri)

		fun onImageShowing(settings: ReaderSettings)

		fun onImageShown()

		fun onProgressChanged(progress: Int)

		fun onConfigChanged()
	}
}
