package org.koitharu.kotatsu.ui.reader.thumbnails

import android.view.ViewGroup
import androidx.core.net.toUri
import coil.Coil
import coil.request.ImageRequest
import coil.size.PixelSize
import coil.size.Size
import kotlinx.android.synthetic.main.item_page_thumb.*
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.local.PagesCache
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder

class PageThumbnailHolder(parent: ViewGroup, private val scope: CoroutineScope) :
	BaseViewHolder<MangaPage, PagesCache>(parent, R.layout.item_page_thumb) {

	private var job: Job? = null
	private val thumbSize: Size

	init {
		val width = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
		thumbSize = PixelSize(
			width = width,
			height = (width * 13f / 18f).toInt()
		)
	}

	override fun onBind(data: MangaPage, extra: PagesCache) {
		imageView_thumb.setImageDrawable(null)
		textView_number.text = (bindingAdapterPosition + 1).toString()
		job?.cancel()
		job = scope.launch(Dispatchers.IO) {
			try {
				val url = data.preview ?: data.url.let {
					val pageUrl = MangaProviderFactory.create(data.source).getPageFullUrl(data)
					extra[pageUrl]?.toUri()?.toString() ?: pageUrl
				}
				val drawable = Coil.execute(
					ImageRequest.Builder(context)
						.data(url)
						.size(thumbSize)
						.build()
				).drawable
				withContext(Dispatchers.Main) {
					imageView_thumb.setImageDrawable(drawable)
				}
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	override fun onRecycled() {
		job?.cancel()
		imageView_thumb.setImageDrawable(null)
	}
}