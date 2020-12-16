package org.koitharu.kotatsu.reader.ui.pager.wetoon

import android.net.Uri
import android.view.View
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.databinding.ItemPageWebtoonBinding
import org.koitharu.kotatsu.reader.ui.PageLoader
import org.koitharu.kotatsu.reader.ui.pager.BasePageHolder
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.utils.ext.getDisplayMessage


class WebtoonHolder(
	binding: ItemPageWebtoonBinding,
	loader: PageLoader,
	settings: AppSettings
) : BasePageHolder<ItemPageWebtoonBinding>(binding, loader, settings), View.OnClickListener {

	private var scrollToRestore = 0

	init {
		binding.ssiv.setOnImageEventListener(delegate)
		binding.buttonRetry.setOnClickListener(this)
	}

	override fun onBind(data: ReaderPage) {
		delegate.onBind(data.toMangaPage())
	}

	override fun onRecycled() {
		delegate.onRecycle()
		binding.ssiv.recycle()
	}

	override fun onLoadingStarted() {
		binding.layoutError.isVisible = false
		binding.progressBar.isVisible = true
		binding.ssiv.recycle()
	}

	override fun onImageReady(uri: Uri) {
		binding.ssiv.setImage(ImageSource.uri(uri))
	}

	override fun onImageShowing(zoom: ZoomMode) {
		with(binding.ssiv) {
			maxScale = 2f * width / sWidth.toFloat()
			setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
			minScale = width / sWidth.toFloat()
			scrollTo(
				when {
					scrollToRestore != 0 -> scrollToRestore
					itemView.top < 0 -> getScrollRange()
					else -> 0
				}
			)
		}
	}

	override fun onImageShown() {
		binding.progressBar.isVisible = false
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return)
		}
	}

	override fun onError(e: Throwable) {
		binding.textViewError.text = e.getDisplayMessage(context.resources)
		binding.layoutError.isVisible = true
		binding.progressBar.isVisible = false
	}

	fun getScrollY() = binding.ssiv.getScroll()

	fun restoreScroll(scroll: Int) {
		if (binding.ssiv.isReady) {
			binding.ssiv.scrollTo(scroll)
		} else {
			scrollToRestore = scroll
		}
	}
}