package org.koitharu.kotatsu.shelf.ui.config.categories

import androidx.core.view.updatePaddingRelative
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemCategoryCheckableMultipleBinding
import org.koitharu.kotatsu.shelf.domain.ShelfSection

fun shelfSectionAD(
	listener: OnListItemClickListener<ShelfConfigModel>,
) = adapterDelegateViewBinding<ShelfConfigModel.Section, ShelfConfigModel, ItemCategoryCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCategoryCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)
	itemView.setOnClickListener(eventListener)

	bind {
		binding.root.setText(item.section.titleResId)
		binding.root.isChecked = item.isChecked
	}
}

fun shelfCategoryAD(
	listener: OnListItemClickListener<ShelfConfigModel>,
) =
	adapterDelegateViewBinding<ShelfConfigModel.FavouriteCategory, ShelfConfigModel, ItemCategoryCheckableMultipleBinding>(
		{ layoutInflater, parent -> ItemCategoryCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
	) {
		val eventListener = AdapterDelegateClickListenerAdapter(this, listener)
		itemView.setOnClickListener(eventListener)
		binding.root.updatePaddingRelative(
			start = binding.root.paddingStart * 2,
			end = binding.root.paddingStart,
		)

		bind {
			binding.root.text = item.title
			binding.root.isChecked = item.isChecked
		}
	}

private val ShelfSection.titleResId: Int
	get() = when (this) {
		ShelfSection.HISTORY -> R.string.history
		ShelfSection.LOCAL -> R.string.local_storage
		ShelfSection.UPDATED -> R.string.updated
		ShelfSection.FAVORITES -> R.string.favourites
	}
