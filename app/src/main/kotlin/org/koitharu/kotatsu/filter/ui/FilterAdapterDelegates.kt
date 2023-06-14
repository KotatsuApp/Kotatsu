package org.koitharu.kotatsu.filter.ui

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.model.titleRes
import org.koitharu.kotatsu.core.util.ext.setChecked
import org.koitharu.kotatsu.databinding.ItemCheckableMultipleBinding
import org.koitharu.kotatsu.databinding.ItemCheckableSingleBinding
import org.koitharu.kotatsu.filter.ui.model.FilterItem
import org.koitharu.kotatsu.list.ui.model.ListModel

fun filterSortDelegate(
	listener: OnFilterChangedListener,
) = adapterDelegateViewBinding<FilterItem.Sort, ListModel, ItemCheckableSingleBinding>(
	{ layoutInflater, parent -> ItemCheckableSingleBinding.inflate(layoutInflater, parent, false) },
) {

	itemView.setOnClickListener {
		listener.onSortItemClick(item)
	}

	bind { payloads ->
		binding.root.setText(item.order.titleRes)
		binding.root.setChecked(item.isSelected, payloads.isNotEmpty())
	}
}

fun filterTagDelegate(
	listener: OnFilterChangedListener,
) = adapterDelegateViewBinding<FilterItem.Tag, ListModel, ItemCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
) {

	itemView.setOnClickListener {
		listener.onTagItemClick(item)
	}

	bind { payloads ->
		binding.root.text = item.tag.title
		binding.root.setChecked(item.isChecked, payloads.isNotEmpty())
	}
}

fun filterErrorDelegate() = adapterDelegate<FilterItem.Error, ListModel>(R.layout.item_sources_empty) {

	bind {
		(itemView as TextView).setText(item.textResId)
	}
}
