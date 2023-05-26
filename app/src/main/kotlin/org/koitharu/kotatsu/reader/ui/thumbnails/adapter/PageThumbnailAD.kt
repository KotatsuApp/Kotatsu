package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.size.Scale
import coil.size.Size
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.decodeRegion
import org.koitharu.kotatsu.core.util.ext.disposeImageRequest
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.setTextColorAttr
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.databinding.ItemPageThumbBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail
import com.google.android.material.R as materialR

fun pageThumbnailAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<PageThumbnail>,
) = adapterDelegateViewBinding<PageThumbnail, ListModel, ItemPageThumbBinding>(
	{ inflater, parent -> ItemPageThumbBinding.inflate(inflater, parent, false) },
) {

	val gridWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.preferred_grid_width)
	val thumbSize = Size(
		width = gridWidth,
		height = (gridWidth / 13f * 18f).toInt(),
	)

	val clickListenerAdapter = AdapterDelegateClickListenerAdapter(this, clickListener)
	binding.root.setOnClickListener(clickListenerAdapter)
	binding.root.setOnLongClickListener(clickListenerAdapter)

	bind {
		val data: Any = item.page.preview?.takeUnless { it.isEmpty() } ?: item.page.toMangaPage()
		binding.imageViewThumb.newImageRequest(lifecycleOwner, data)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			size(thumbSize)
			scale(Scale.FILL)
			allowRgb565(true)
			decodeRegion(0)
			source(item.page.source)
			enqueueWith(coil)
		}
		with(binding.textViewNumber) {
			setBackgroundResource(if (item.isCurrent) R.drawable.bg_badge_accent else R.drawable.bg_badge_empty)
			setTextColorAttr(if (item.isCurrent) materialR.attr.colorOnTertiary else android.R.attr.textColorPrimary)
			text = (item.number).toString()
		}
	}

	onViewRecycled {
		binding.imageViewThumb.disposeImageRequest()
	}
}
