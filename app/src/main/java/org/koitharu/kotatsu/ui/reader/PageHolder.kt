package org.koitharu.kotatsu.ui.reader

import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class PageHolder(parent: ViewGroup, private val loader: PageLoader) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page),
	SubsamplingScaleImageView.OnImageEventListener {

	init {
		ssiv.setOnImageEventListener(this)
		button_retry.setOnClickListener {
			onBind(boundData ?: return@setOnClickListener, Unit)
		}
	}

	override fun onBind(data: MangaPage, extra: Unit) {
		layout_error.isVisible = false
		progressBar.isVisible = true
		ssiv.recycle()
		loader.load(data.url) {
			val uri = it.getOrNull()?.toUri()
			if (uri != null) {
				ssiv.setImage(ImageSource.uri(uri))
			}
			val error = it.exceptionOrNull()
			if (error != null) {
				onError(error)
			}
		}
	}

	override fun onReady() = Unit

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