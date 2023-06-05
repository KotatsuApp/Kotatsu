package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.databinding.ItemHeaderButtonBinding
import org.koitharu.kotatsu.databinding.ItemHeaderSingleBinding
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel

fun listHeaderAD(
	listener: ListHeaderClickListener?,
) = adapterDelegateViewBinding<ListHeader, ListModel, ItemHeaderButtonBinding>(
	{ inflater, parent -> ItemHeaderButtonBinding.inflate(inflater, parent, false) },
) {
	if (listener != null) {
		binding.buttonMore.setOnClickListener {
			listener.onListHeaderClick(item, it)
		}
	}

	bind {
		binding.textViewTitle.text = item.getText(context)
		binding.buttonMore.setTextAndVisible(item.buttonTextRes)
	}
}

fun listSimpleHeaderAD() = adapterDelegateViewBinding<ListHeader, ListModel, ItemHeaderSingleBinding>(
	{ inflater, parent -> ItemHeaderSingleBinding.inflate(inflater, parent, false) },
) {

	bind {
		binding.textViewTitle.text = item.getText(context)
	}
}
