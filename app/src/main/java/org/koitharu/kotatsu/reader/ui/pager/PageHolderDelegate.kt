package org.koitharu.kotatsu.reader.ui.pager

import android.net.Uri
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.*
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.utils.ext.launchAfter
import org.koitharu.kotatsu.utils.ext.launchInstead
import java.io.File
import java.io.IOException

class PageHolderDelegate(
	private val loader: PageLoader,
	private val settings: AppSettings,
	private val callback: Callback
) : SubsamplingScaleImageView.DefaultOnImageEventListener(), CoroutineScope by loader {

	private var state = State.EMPTY
	private var job: Job? = null
	private var file: File? = null

	fun onBind(page: MangaPage) {
		doLoad(page, force = false)
	}

	fun retry(page: MangaPage) {
		doLoad(page, force = true)
	}

	fun onRecycle() {
		state = State.EMPTY
		file = null
		job?.cancel()
	}

	override fun onReady() {
		state = State.SHOWING
		callback.onImageShowing(settings.zoomMode)
	}

	override fun onImageLoaded() {
		state = State.SHOWN
		callback.onImageShown()
	}

	override fun onImageLoadError(e: Exception) {
		val file = this.file
		if (state == State.LOADED && e is IOException && file != null && file.exists()) {
			job = launchAfter(job) {
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
		} else {
			state = State.ERROR
			callback.onError(e)
		}
	}

	private fun doLoad(data: MangaPage, force: Boolean) {
		job = launchInstead(job) {
			state = State.LOADING
			callback.onLoadingStarted()
			try {
				val file = withContext(Dispatchers.IO) {
					val pageUrl = data.source.repository.getPageFullUrl(data)
					check(pageUrl.isNotEmpty()) { "Cannot obtain full image url" }
					loader.loadFile(pageUrl, force)
				}
				this@PageHolderDelegate.file = file
				state = State.LOADED
				callback.onImageReady(file.toUri())
			} catch (e: CancellationException) {
				// do nothing
			} catch (e: Exception) {
				state = State.ERROR
				callback.onError(e)
			}
		}
	}

	private enum class State {
		EMPTY, LOADING, LOADED, CONVERTING, CONVERTED, SHOWING, SHOWN, ERROR
	}

	interface Callback {

		fun onLoadingStarted()

		fun onError(e: Throwable)

		fun onImageReady(uri: Uri)

		fun onImageShowing(zoom: ZoomMode)

		fun onImageShown()
	}
}