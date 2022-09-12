package org.koitharu.kotatsu.reader.ui.pager

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings

class PageHolderDelegate(
	private val loader: PageLoader,
	private val readerSettings: ReaderSettings,
	private val callback: Callback,
	private val exceptionResolver: ExceptionResolver,
) : DefaultOnImageEventListener, Observer<ReaderSettings> {

	private val scope = loader.loaderScope + Dispatchers.Main.immediate
	private var state = State.EMPTY
	private var job: Job? = null
	private var file: File? = null
	private var error: Throwable? = null

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
		val file = this.file
		error = e
		if (state == State.LOADED && e is IOException && file != null && file.exists()) {
			tryConvert(file, e)
		} else {
			state = State.ERROR
			callback.onError(e)
		}
	}

	override fun onChanged(t: ReaderSettings?) {
		if (state == State.SHOWN) {
			callback.onImageShowing(readerSettings)
		}
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
			} catch (e2: Throwable) {
				e.addSuppressed(e2)
				state = State.ERROR
				callback.onError(e)
			}
		}
	}

	private suspend fun CoroutineScope.doLoad(data: MangaPage, force: Boolean) {
		state = State.LOADING
		error = null
		callback.onLoadingStarted()
		try {
			val task = loader.loadPageAsync(data, force)
			val progressObserver = observeProgress(this, task.progressAsFlow())
			val file = task.await()
			progressObserver.cancel()
			this@PageHolderDelegate.file = file
			state = State.LOADED
			callback.onImageReady(file.toUri())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			state = State.ERROR
			error = e
			callback.onError(e)
		}
	}

	private fun observeProgress(scope: CoroutineScope, progress: Flow<Float>) = progress
		.debounce(500)
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
	}
}
