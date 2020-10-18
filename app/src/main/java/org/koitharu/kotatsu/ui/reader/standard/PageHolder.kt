package org.koitharu.kotatsu.ui.reader.standard

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import kotlinx.android.synthetic.main.item_page.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.base.PageHolderDelegate
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class PageHolder(parent: ViewGroup, loader: PageLoader) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page),
	PageHolderDelegate.Callback, View.OnClickListener {

	private val delegate = PageHolderDelegate(loader, this)

	init {
		ssiv.setOnImageEventListener(delegate)
		button_retry.setOnClickListener(this)
	}

	override fun onBind(data: MangaPage, extra: Unit) {
		delegate.onBind(data)
	}

	override fun onRecycled() {
		delegate.onRecycle()
		ssiv.recycle()
	}

	override fun onLoadingStarted() {
		layout_error.isVisible = false
		progressBar.isVisible = true
		ssiv.recycle()
	}

	override fun onImageReady(uri: Uri) {
		ssiv.setImage(ImageSource.uri(uri))
	}

	override fun onImageShowing() {
		ssiv.maxScale = 2f * maxOf(
			ssiv.width / ssiv.sWidth.toFloat(),
			ssiv.height / ssiv.sHeight.toFloat()
		)
		ssiv.resetScaleAndCenter()
	}

	override fun onImageShown() {
		progressBar.isVisible = false
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData ?: return)
		}
	}

	override fun onError(e: Throwable) {
		textView_error.text = e.getDisplayMessage(context.resources)
		layout_error.isVisible = true
		progressBar.isVisible = false
	}
}