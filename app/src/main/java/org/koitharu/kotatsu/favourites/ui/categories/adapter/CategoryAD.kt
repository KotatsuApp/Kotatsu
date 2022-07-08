package org.koitharu.kotatsu.favourites.ui.categories.adapter

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import coil.size.Scale
import coil.util.CoilUtils
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.databinding.ItemCategoryBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun categoryAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<FavouriteCategory>
) = adapterDelegateViewBinding<CategoryListModel, ListModel, ItemCategoryBinding>(
	{ inflater, parent -> ItemCategoryBinding.inflate(inflater, parent, false) }
) {

	val eventListener = object : OnClickListener, OnLongClickListener {
		override fun onClick(v: View) = clickListener.onItemClick(item.category, v)
		override fun onLongClick(v: View) = clickListener.onItemLongClick(item.category, v)
	}
	val coverViews = arrayOf(binding.imageViewCover1, binding.imageViewCover2, binding.imageViewCover3)
	val imageRequests = arrayOfNulls<Disposable?>(coverViews.size)
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)

	bind {
		imageRequests.forEach { it?.dispose() }
		binding.textViewTitle.text = item.category.title
		binding.textViewSubtitle.text = context.resources.getQuantityString(
			R.plurals.items,
			item.mangaCount,
			item.mangaCount,
		)
		repeat(coverViews.size) { i ->
			imageRequests[i] = coverViews[i].newImageRequest(item.covers.getOrNull(i))
				.placeholder(R.drawable.ic_placeholder)
				.fallback(null)
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