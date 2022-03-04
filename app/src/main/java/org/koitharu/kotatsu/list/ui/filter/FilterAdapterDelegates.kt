package org.koitharu.kotatsu.list.ui.filter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemCheckableMultipleBinding
import org.koitharu.kotatsu.databinding.ItemCheckableSingleBinding
import org.koitharu.kotatsu.databinding.ItemFilterHeaderBinding

fun filterSortDelegate(
	listener: OnFilterChangedListener,
) = adapterDelegateViewBinding<FilterItem.Sort, FilterItem, ItemCheckableSingleBinding>(
	{ layoutInflater, parent -> ItemCheckableSingleBinding.inflate(layoutInflater, parent, false) }
) {

	itemView.setOnClickListener {
		listener.onSortItemClick(item)
	}

	bind {
		binding.root.setText(item.order.titleRes)
		binding.root.isChecked = item.isSelected
	}
}

fun filterTagDelegate(
	listener: OnFilterChangedListener,
) = adapterDelegateViewBinding<FilterItem.Tag, FilterItem, ItemCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCheckableMultipleBinding.inflate(layoutInflater, parent, false) }
) {

	itemView.setOnClickListener {
		listener.onTagItemClick(item)
	}

	bind {
		binding.root.text = item.tag.title
		binding.root.isChecked = item.isChecked
	}
}

fun filterHeaderDelegate() = adapterDelegateViewBinding<FilterItem.Header, FilterItem, ItemFilterHeaderBinding>(
	{ layoutInflater, parent -> ItemFilterHeaderBinding.inflate(layoutInflater, parent, false) }
) {

	bind {
		binding.root.setText(item.titleResId)
	}
}

fun filterLoadingDelegate() = adapterDelegate<FilterItem.Loading, FilterItem>(R.layout.item_loading_footer) {}

fun filterErrorDelegate() = adapterDelegate<FilterItem.Error, FilterItem>(R.layout.item_sources_empty) {

	bind {
		(itemView as TextView).setText(item.textResId)
	}
}