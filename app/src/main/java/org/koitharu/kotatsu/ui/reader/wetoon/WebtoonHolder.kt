package org.koitharu.kotatsu.ui.reader.wetoon

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.item_page_webtoon.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.ui.base.list.BaseViewHolder
import org.koitharu.kotatsu.ui.reader.PageLoader
import org.koitharu.kotatsu.ui.reader.base.PageHolderDelegate
import org.koitharu.kotatsu.utils.ext.getDisplayMessage


class WebtoonHolder(parent: ViewGroup, private val loader: PageLoader) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page_webtoon),
	PageHolderDelegate.Callback, View.OnClickListener {

	private val delegate = PageHolderDelegate(loader, this)
	private var scrollToRestore = 0

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
		ssiv.maxScale = 2f * ssiv.width / ssiv.sWidth.toFloat()
		ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
		ssiv.minScale = ssiv.width / ssiv.sWidth.toFloat()
		ssiv.scrollTo(
			when {
				scrollToRestore != 0 -> scrollToRestore
				itemView.top < 0 -> ssiv.getScrollRange()
				else -> 0
			}
		)
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

	fun getScrollY() = ssiv.getScroll()

	fun restoreScroll(scroll: Int) {
		if (ssiv.isReady) {
			ssiv.scrollTo(scroll)
		} else {
			scrollToRestore = scroll
		}
	}
}