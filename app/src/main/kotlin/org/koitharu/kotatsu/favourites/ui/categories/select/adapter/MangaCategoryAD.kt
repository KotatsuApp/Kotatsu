package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.setChecked
import org.koitharu.kotatsu.databinding.ItemCategoryCheckableBinding
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

fun mangaCategoryAD(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) = adapterDelegateViewBinding<MangaCategoryItem, ListModel, ItemCategoryCheckableBinding>(
	{ inflater, parent -> ItemCategoryCheckableBinding.inflate(inflater, parent, false) },
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, itemView)
	}

	bind { payloads ->
		binding.root.isEnabled = item.isEnabled
		binding.checkableImageView.isEnabled = item.isEnabled
		binding.checkableImageView.setChecked(item.isChecked, ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED in payloads)
		binding.textViewTitle.text = item.category.title
		binding.imageViewTracker.isVisible = item.category.isTrackingEnabled && item.isTrackerEnabled
		binding.imageViewHidden.isGone = item.category.isVisibleInLibrary
	}
}
