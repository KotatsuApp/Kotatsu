package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.databinding.ItemHeaderButtonBinding
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.setTextAndVisible

fun listHeaderAD(
	listener: ListHeaderClickListener,
) = adapterDelegateViewBinding<ListHeader, ListModel, ItemHeaderButtonBinding>(
	{ inflater, parent -> ItemHeaderButtonBinding.inflate(inflater, parent, false) },
) {
	binding.buttonMore.setOnClickListener {
		listener.onListHeaderClick(item, it)
	}

	bind {
		binding.textViewTitle.text = item.getText(context)
		binding.buttonMore.setTextAndVisible(item.buttonTextRes)
	}
}
