package org.koitharu.kotatsu.list.ui.filter

import android.widget.TextView
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.model.titleRes
import org.koitharu.kotatsu.databinding.ItemCheckableNewBinding
import org.koitharu.kotatsu.databinding.ItemFilterHeaderBinding

fun filterSortDelegate(
	listener: OnFilterChangedListener,
) = adapterDelegateViewBinding<FilterItem.Sort, FilterItem, ItemCheckableNewBinding>(
	{ layoutInflater, parent -> ItemCheckableNewBinding.inflate(layoutInflater, parent, false) },
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
) = adapterDelegateViewBinding<FilterItem.Tag, FilterItem, ItemCheckableNewBinding>(
	{ layoutInflater, parent -> ItemCheckableNewBinding.inflate(layoutInflater, parent, false) },
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
	{ layoutInflater, parent -> ItemFilterHeaderBinding.inflate(layoutInflater, parent, false) },
) {

	bind {
		binding.textViewTitle.setText(item.titleResId)
		binding.badge.isVisible = if (item.counter == 0) {
			false
		} else {
			binding.badge.text = item.counter.toString()
			true
		}
	}
}

fun filterLoadingDelegate() = adapterDelegate<FilterItem.Loading, FilterItem>(R.layout.item_loading_footer) {}

fun filterErrorDelegate() = adapterDelegate<FilterItem.Error, FilterItem>(R.layout.item_sources_empty) {

	bind {
		(itemView as TextView).setText(item.textResId)
	}
}
