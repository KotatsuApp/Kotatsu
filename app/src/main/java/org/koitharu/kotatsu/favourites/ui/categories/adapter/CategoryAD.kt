package org.koitharu.kotatsu.favourites.ui.categories.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import coil.size.Scale
import coil.util.CoilUtils
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemCategoryBinding
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesListListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.animatorDurationScale
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun categoryAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: FavouriteCategoriesListListener,
) = adapterDelegateViewBinding<CategoryListModel, ListModel, ItemCategoryBinding>(
	{ inflater, parent -> ItemCategoryBinding.inflate(inflater, parent, false) }
) {

	val eventListener = object : OnClickListener, OnLongClickListener, OnTouchListener {
		override fun onClick(v: View) = clickListener.onItemClick(item.category, v)
		override fun onLongClick(v: View) = clickListener.onItemLongClick(item.category, v)
		override fun onTouch(v: View?, event: MotionEvent): Boolean = item.isReorderMode &&
			event.actionMasked == MotionEvent.ACTION_DOWN &&
			clickListener.onDragHandleTouch(this@adapterDelegateViewBinding)
	}
	val backgroundColor = context.getThemeColor(android.R.attr.colorBackground)
	ImageViewCompat.setImageTintList(
		binding.imageViewCover3,
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153))
	)
	ImageViewCompat.setImageTintList(
		binding.imageViewCover2,
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76))
	)
	binding.imageViewCover2.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76))
	binding.imageViewCover3.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153))
	val fallback = ColorDrawable(Color.TRANSPARENT)
	val coverViews = arrayOf(binding.imageViewCover1, binding.imageViewCover2, binding.imageViewCover3)
	val imageRequests = arrayOfNulls<Disposable?>(coverViews.size)
	val crossFadeDuration = (context.resources.getInteger(R.integer.config_defaultAnimTime) *
		context.animatorDurationScale).toInt()
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)
	itemView.setOnTouchListener(eventListener)

	bind {
		imageRequests.forEach { it?.dispose() }
		binding.imageViewHandle.isVisible = item.isReorderMode
		binding.textViewTitle.text = item.category.title
		binding.textViewSubtitle.text = context.resources.getQuantityString(
			R.plurals.items,
			item.mangaCount,
			item.mangaCount,
		)
		repeat(coverViews.size) { i ->
			imageRequests[i] = coverViews[i].newImageRequest(item.covers.getOrNull(i))
				.placeholder(R.drawable.ic_placeholder)
				.crossfade(crossFadeDuration * (i + 1))
				.fallback(fallback)
				.error(R.drawable.ic_placeholder)
				.scale(Scale.FILL)
				.allowRgb565(true)
				.lifecycle(lifecycleOwner)
				.enqueueWith(coil)
		}
	}

	onViewRecycled {
		repeat(coverViews.size) { i ->
			imageRequests[i]?.dispose()
			imageRequests[i] = null
			CoilUtils.dispose(coverViews[i])
			coverViews[i].setImageDrawable(null)
		}
	}
}