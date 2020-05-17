package org.koitharu.kotatsu.ui.reader.wetoon

import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page_webtoon.*
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.utils.ext.getDisplayMessage


class WebtoonHolder(parent: ViewGroup, private val loader: PageLoader) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page_webtoon),
	SubsamplingScaleImageView.OnImageEventListener, CoroutineScope by loader {

	private var job: Job? = null
	private var scrollToRestore = 0

	init {
		ssiv.setOnImageEventListener(this)
		button_retry.setOnClickListener {
			doLoad(boundData ?: return@setOnClickListener, force = true)
		}
	}

	override fun onBind(data: MangaPage, extra: Unit) {
		doLoad(data, force = false)
	}

	private fun doLoad(data: MangaPage, force: Boolean) {
		job?.cancel()
		scrollToRestore = 0
		job = launch {
			layout_error.isVisible = false
			progressBar.isVisible = true
			ssiv.recycle()
			try {
				val uri = withContext(Dispatchers.IO) {
					val pageUrl = MangaProviderFactory.create(data.source).getPageFullUrl(data)
					loader.loadFile(pageUrl, force)
				}.toUri()
				ssiv.setImage(ImageSource.uri(uri))
			} catch (e: CancellationException) {
				//do nothing
			} catch (e: Exception) {
				onError(e)
			}
		}
	}

	override fun onRecycled() {
		job?.cancel()
		ssiv.recycle()
	}

	fun getScrollY() = ssiv.getScroll()

	fun restoreScroll(scroll: Int) {
		if (ssiv.isReady) {
			ssiv.scrollTo(scroll)
		} else {
			scrollToRestore = scroll
		}
	}

	override fun onReady() {
		ssiv.maxScale = 2f * ssiv.width / ssiv.sWidth.toFloat()
		ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
		ssiv.minScale = ssiv.width / ssiv.sWidth.toFloat()
		ssiv.scrollTo(when {
			scrollToRestore != 0 -> scrollToRestore
			itemView.top < 0 -> ssiv.getScrollRange()
			else -> 0
		})
	}

	override fun onImageLoadError(e: Exception) = onError(e)

	override fun onImageLoaded() {
		progressBar.isVisible = false
	}

	override fun onTileLoadError(e: Exception?) = Unit

	override fun onPreviewReleased() = Unit

	override fun onPreviewLoadError(e: Exception?) = Unit

	private fun onError(e: Throwable) {
		textView_error.text = e.getDisplayMessage(context.resources)
		layout_error.isVisible = true
		progressBar.isVisible = false
	}
}