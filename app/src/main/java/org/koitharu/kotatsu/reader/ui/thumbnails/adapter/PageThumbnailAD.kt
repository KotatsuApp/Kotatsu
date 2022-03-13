package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import androidx.core.net.toUri
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.PixelSize
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.databinding.ItemPageThumbBinding
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail
import org.koitharu.kotatsu.utils.ext.IgnoreErrors
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.setTextColorAttr
import com.google.android.material.R as materialR

fun pageThumbnailAD(
	coil: ImageLoader,
	scope: CoroutineScope,
	cache: PagesCache,
	clickListener: OnListItemClickListener<MangaPage>
) = adapterDelegateViewBinding<PageThumbnail, PageThumbnail, ItemPageThumbBinding>(
	{ inflater, parent -> ItemPageThumbBinding.inflate(inflater, parent, false) }
) {

	var job: Job? = null
	val gridWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
	val thumbSize = PixelSize(
		width = gridWidth,
		height = (gridWidth * 13f / 18f).toInt()
	)

	binding.root.setOnClickListener {
		clickListener.onItemClick(item.page, itemView)
	}

	bind {
		job?.cancel()
		binding.imageViewThumb.setImageDrawable(null)
		with(binding.textViewNumber) {
			setBackgroundResource(if (item.isCurrent) R.drawable.bg_badge_accent else R.drawable.bg_badge_empty)
			setTextColorAttr(if (item.isCurrent) materialR.attr.colorOnTertiary else android.R.attr.textColorPrimary)
			text = (item.number).toString()
		}
		job = scope.launch(Dispatchers.Default + IgnoreErrors) {
			val url = item.page.preview ?: item.page.url.let {
				val pageUrl = item.repository.getPageUrl(item.page)
				cache[pageUrl]?.toUri()?.toString() ?: pageUrl
			}
			val drawable = coil.execute(
				ImageRequest.Builder(context)
					.data(url)
					.referer(item.page.referer)
					.size(thumbSize)
					.allowRgb565(true)
					.build()
			).drawable
			withContext(Dispatchers.Main) {
				binding.imageViewThumb.setImageDrawable(drawable)
			}
		}
	}

	onViewRecycled {
		job?.cancel()
		binding.imageViewThumb.setImageDrawable(null)
	}
}