package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import com.google.android.material.R as materialR
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemPageThumbBinding
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import org.koitharu.kotatsu.utils.ext.setTextColorAttr

fun pageThumbnailAD(
	coil: ImageLoader,
	scope: CoroutineScope,
	loader: PageLoader,
	clickListener: OnListItemClickListener<MangaPage>,
) = adapterDelegateViewBinding<PageThumbnail, PageThumbnail, ItemPageThumbBinding>(
	{ inflater, parent -> ItemPageThumbBinding.inflate(inflater, parent, false) }
) {
	var job: Job? = null
	val gridWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
	val thumbSize = Size(
		width = gridWidth,
		height = (gridWidth * 13f / 18f).toInt()
	)

	suspend fun loadPageThumbnail(item: PageThumbnail): Drawable? = withContext(Dispatchers.Default) {
		item.page.preview?.let { url ->
			coil.execute(
				ImageRequest.Builder(context)
					.data(url)
					.referer(item.page.referer)
					.size(thumbSize)
					.scale(Scale.FILL)
					.allowRgb565(true)
					.build()
			).drawable
		}?.let { drawable ->
			return@withContext drawable
		}
		val file = loader.loadPage(item.page, force = false)
		coil.execute(
			ImageRequest.Builder(context)
				.data(file)
				.size(thumbSize)
				.allowRgb565(true)
				.build()
		).drawable
	}

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
		job = scope.launch {
			val drawable = runCatchingCancellable {
				loadPageThumbnail(item)
			}.getOrNull()
			binding.imageViewThumb.setImageDrawable(drawable)
		}
	}

	onViewRecycled {
		job?.cancel()
		job = null
		binding.imageViewThumb.setImageDrawable(null)
	}
}