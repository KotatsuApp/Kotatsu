package org.koitharu.kotatsu.favourites.ui.categories.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.databinding.ItemCategoriesAllBinding
import org.koitharu.kotatsu.databinding.ItemCategoryBinding
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesListListener
import org.koitharu.kotatsu.list.ui.model.ListModel

@SuppressLint("ClickableViewAccessibility")
fun categoryAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: FavouriteCategoriesListListener,
) = adapterDelegateViewBinding<CategoryListModel, ListModel, ItemCategoryBinding>(
	{ inflater, parent -> ItemCategoryBinding.inflate(inflater, parent, false) },
) {
	val eventListener = object : OnClickListener, OnLongClickListener, OnTouchListener {
		override fun onClick(v: View) = if (v.id == R.id.imageView_edit) {
			clickListener.onEditClick(item.category, v)
		} else {
			clickListener.onItemClick(item.category, v)
		}

		override fun onLongClick(v: View) = clickListener.onItemLongClick(item.category, v)
		override fun onTouch(v: View?, event: MotionEvent): Boolean = event.actionMasked == MotionEvent.ACTION_DOWN &&
			clickListener.onDragHandleTouch(this@adapterDelegateViewBinding)
	}
	val backgroundColor = context.getThemeColor(android.R.attr.colorBackground)
	ImageViewCompat.setImageTintList(
		binding.imageViewCover3,
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153)),
	)
	ImageViewCompat.setImageTintList(
		binding.imageViewCover2,
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76)),
	)
	binding.imageViewCover2.backgroundTintList =
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76))
	binding.imageViewCover3.backgroundTintList =
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153))
	val fallback = ColorDrawable(Color.TRANSPARENT)
	val coverViews = arrayOf(binding.imageViewCover1, binding.imageViewCover2, binding.imageViewCover3)
	val crossFadeDuration = context.getAnimationDuration(R.integer.config_defaultAnimTime).toInt()
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)
	binding.imageViewEdit.setOnClickListener(eventListener)
	binding.imageViewHandle.setOnTouchListener(eventListener)

	bind {
		binding.imageViewHandle.isVisible = item.isActionsEnabled
		binding.imageViewEdit.isVisible = item.isActionsEnabled
		binding.textViewTitle.text = item.category.title
		binding.textViewSubtitle.text = if (item.mangaCount == 0) {
			getString(R.string.empty)
		} else {
			context.resources.getQuantityString(
				R.plurals.items,
				item.mangaCount,
				item.mangaCount,
			)
		}
		binding.imageViewTracker.isVisible = item.category.isTrackingEnabled
		binding.imageViewHidden.isGone = item.category.isVisibleInLibrary
		repeat(coverViews.size) { i ->
			val cover = item.covers.getOrNull(i)
			coverViews[i].newImageRequest(lifecycleOwner, cover?.url)?.run {
				placeholder(R.drawable.ic_placeholder)
				fallback(fallback)
				mangaSourceExtra(cover?.mangaSource)
				crossfade(crossFadeDuration * (i + 1))
				error(R.drawable.ic_error_placeholder)
				allowRgb565(true)
				enqueueWith(coil)
			}
		}
	}
}

fun allCategoriesAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: FavouriteCategoriesListListener,
) = adapterDelegateViewBinding<AllCategoriesListModel, ListModel, ItemCategoriesAllBinding>(
	{ inflater, parent -> ItemCategoriesAllBinding.inflate(inflater, parent, false) },
) {
	val eventListener = OnClickListener { v ->
		if (v.id == R.id.imageView_visible) {
			clickListener.onShowAllClick(!item.isVisible)
		} else {
			clickListener.onItemClick(null, v)
		}
	}
	val backgroundColor = context.getThemeColor(android.R.attr.colorBackground)
	ImageViewCompat.setImageTintList(
		binding.imageViewCover3,
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153)),
	)
	ImageViewCompat.setImageTintList(
		binding.imageViewCover2,
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76)),
	)
	binding.imageViewCover2.backgroundTintList =
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 76))
	binding.imageViewCover3.backgroundTintList =
		ColorStateList.valueOf(ColorUtils.setAlphaComponent(backgroundColor, 153))
	val fallback = ColorDrawable(Color.TRANSPARENT)
	val coverViews = arrayOf(binding.imageViewCover1, binding.imageViewCover2, binding.imageViewCover3)
	val crossFadeDuration = context.getAnimationDuration(R.integer.config_defaultAnimTime).toInt()
	itemView.setOnClickListener(eventListener)
	binding.imageViewVisible.setOnClickListener(eventListener)

	bind {
		binding.textViewSubtitle.text = if (item.mangaCount == 0) {
			getString(R.string.empty)
		} else {
			context.resources.getQuantityString(
				R.plurals.items,
				item.mangaCount,
				item.mangaCount,
			)
		}
		binding.imageViewVisible.isVisible = item.isActionsEnabled
		binding.imageViewVisible.setImageResource(
			if (item.isVisible) {
				R.drawable.ic_eye
			} else {
				R.drawable.ic_eye_off
			},
		)
		repeat(coverViews.size) { i ->
			val cover = item.covers.getOrNull(i)
			coverViews[i].newImageRequest(lifecycleOwner, cover?.url)?.run {
				placeholder(R.drawable.ic_placeholder)
				fallback(fallback)
				mangaSourceExtra(cover?.mangaSource)
				crossfade(crossFadeDuration * (i + 1))
				error(R.drawable.ic_error_placeholder)
				allowRgb565(true)
				enqueueWith(coil)
			}
		}
	}
}
