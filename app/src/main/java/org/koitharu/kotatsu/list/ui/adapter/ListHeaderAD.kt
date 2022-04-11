package org.koitharu.kotatsu.list.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.titleRes
import org.koitharu.kotatsu.databinding.ItemHeaderWithFilterBinding
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel

fun listHeaderAD() = adapterDelegate<ListHeader, ListModel>(
	layout = R.layout.item_header,
	on = { item, _, _ -> item is ListHeader && item.sortOrder == null },
) {

	bind {
		val textView = (itemView as TextView)
		if (item.text != null) {
			textView.text = item.text
		} else {
			textView.setText(item.textRes)
		}
	}
}

fun listHeaderWithFilterAD(
	listener: MangaListListener,
) = adapterDelegateViewBinding<ListHeader, ListModel, ItemHeaderWithFilterBinding>(
	viewBinding = { inflater, parent -> ItemHeaderWithFilterBinding.inflate(inflater, parent, false) },
	on = { item, _, _ -> item is ListHeader && item.sortOrder != null },
) {

	binding.textViewFilter.setOnClickListener {
		listener.onFilterClick()
	}

	bind {
		if (item.text != null) {
			binding.textViewTitle.text = item.text
		} else {
			binding.textViewTitle.setText(item.textRes)
		}
		binding.textViewFilter.setText(requireNotNull(item.sortOrder).titleRes)
	}
}