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
import org.koitharu.kotatsu.utils.ext.IgnoreErrors

fun pageThumbnailAD(
	coil: ImageLoader,
	scope: CoroutineScope,
	cache: PagesCache,
	clickListener: OnListItemClickListener<MangaPage>
) = adapterDelegateViewBinding<MangaPage, MangaPage, ItemPageThumbBinding>(
	{ inflater, parent -> ItemPageThumbBinding.inflate(inflater, parent, false) }
) {

	var job: Job? = null
	val gridWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
	val thumbSize = PixelSize(
		width = gridWidth,
		height = (gridWidth * 13f / 18f).toInt()
	)

	binding.handle.setOnClickListener {
		clickListener.onItemClick(item, itemView)
	}

	bind {
		job?.cancel()
		binding.imageViewThumb.setImageDrawable(null)
		binding.textViewNumber.text = (bindingAdapterPosition + 1).toString()
		job = scope.launch(Dispatchers.Default + IgnoreErrors) {
			val url = item.preview ?: item.url.let {
				val pageUrl = item.source.repository.getPageFullUrl(item)
				cache[pageUrl]?.toUri()?.toString() ?: pageUrl
			}
			val drawable = coil.execute(
				ImageRequest.Builder(context)
					.data(url)
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