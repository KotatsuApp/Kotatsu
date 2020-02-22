package org.koitharu.kotatsu.ui.reader.thumbnails

import android.view.ViewGroup
import coil.Coil
import coil.api.get
import kotlinx.android.synthetic.main.item_page_thumb.*
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.utils.DrawUtils
import org.koitharu.kotatsu.utils.ext.resolveDp

class PageThumbnailHolder(parent: ViewGroup, private val scope: CoroutineScope) :
	BaseViewHolder<MangaPage, Unit>(parent, R.layout.item_page_thumb) {

	private var job: Job? = null

	init {
		val color = DrawUtils.invertColor(textView_number.currentTextColor)
		textView_number.setShadowLayer(parent.resources.resolveDp(26f), 0f, 0f, color)
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