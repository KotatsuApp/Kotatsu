package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.databinding.ItemErrorFooterBinding
import org.koitharu.kotatsu.list.ui.model.ErrorFooter
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

fun errorFooterAD(
	onRetryClick: () -> Unit
) = adapterDelegateViewBinding<ErrorFooter, ListModel, ItemErrorFooterBinding>(
	{ inflater, parent -> ItemErrorFooterBinding.inflate(inflater, parent, false) }
) {

	binding.root.setOnClickListener {
		onRetryClick()
	}

	bind {
		binding.textViewTitle.text = item.exception.getDisplayMessage(context.resources)
		binding.imageViewIcon.setImageResource(item.icon)
	}
}