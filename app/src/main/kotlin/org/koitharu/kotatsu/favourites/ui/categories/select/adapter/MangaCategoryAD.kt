package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import androidx.core.text.buildSpannedString
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.appendIcon
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
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
		binding.checkBox.checkedState = item.checkedState
		if (ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED !in payloads) {
			binding.checkBox.text = buildSpannedString {
				append(item.category.title)
				if (item.isTrackerEnabled && item.category.isTrackingEnabled) {
					append(' ')
					appendIcon(binding.checkBox, R.drawable.ic_notification)
				}
				if (!item.category.isVisibleInLibrary) {
					append(' ')
					appendIcon(binding.checkBox, R.drawable.ic_eye_off)
				}
			}
			binding.checkBox.jumpDrawablesToCurrentState()
		}
	}
}
