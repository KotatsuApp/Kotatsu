package org.koitharu.kotatsu.ui.reader.thumbnails

import android.view.ViewGroup
import coil.Coil
import coil.api.get
import coil.size.PixelSize
import coil.size.Size
import kotlinx.android.synthetic.main.item_page_thumb.*
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class PageThumbnailHolder(parent: ViewGroup, private val scope: CoroutineScope) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page_thumb) {

	private var job: Job? = null
	private	val thumbSize: Size

	init {
//		FIXME
//		val color = DrawUtils.invertColor(textView_number.currentTextColor)
//		textView_number.setShadowLayer(parent.resources.resolveDp(26f), 0f, 0f, color)
		val width = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
		thumbSize = PixelSize(
			width = width,
			height = (width * 13f / 18f).toInt()
		)
	}

	override fun onBind(data: MangaPage, extra: Unit) {
		imageView_thumb.setImageDrawable(null)
		textView_number.text = (adapterPosition + 1).toString()
		job?.cancel()
		job = scope.launch(Dispatchers.IO) {
			try {
				val url = data.preview ?: data.url.let {
					MangaProviderFactory.create(data.source).getPageFullUrl(data)
				}
				val drawable = Coil.get(url) {
					size(thumbSize)
				}
				withContext(Dispatchers.Main) {
					imageView_thumb.setImageDrawable(drawable)
				}
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
}