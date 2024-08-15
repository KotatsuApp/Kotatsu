package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.joinToStringWithLimit
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.databinding.ItemCategoriesHeaderBinding
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.model.CategoriesHeaderItem
import org.koitharu.kotatsu.list.ui.model.ListModel

fun categoriesHeaderAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<CategoriesHeaderItem, ListModel, ItemCategoriesHeaderBinding>(
	{ inflater, parent -> ItemCategoriesHeaderBinding.inflate(inflater, parent, false) },
) {

	val onClickListener = View.OnClickListener { v ->
		val intent = when (v.id) {
			R.id.chip_create -> FavouritesCategoryEditActivity.newIntent(v.context)
			R.id.chip_manage -> Intent(v.context, FavouriteCategoriesActivity::class.java)
			else -> return@OnClickListener
		}
		v.context.startActivity(intent)
	}

	binding.chipCreate.setOnClickListener(onClickListener)
	binding.chipManage.setOnClickListener(onClickListener)

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

	bind {
		binding.textViewTitle.text = item.titles.joinToStringWithLimit(context, 120) { it }

		repeat(coverViews.size) { i ->
			val cover = item.covers.getOrNull(i)
			coverViews[i].newImageRequest(lifecycleOwner, cover?.url)?.run {
				placeholder(R.drawable.ic_placeholder)
				fallback(fallback)
				source(cover?.mangaSource)
				crossfade(crossFadeDuration * (i + 1))
				error(R.drawable.ic_error_placeholder)
				allowRgb565(true)
				enqueueWith(coil)
			}
		}
	}
}
