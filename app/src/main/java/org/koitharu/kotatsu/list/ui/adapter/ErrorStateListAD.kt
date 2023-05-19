package org.koitharu.kotatsu.list.ui.adapter

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.databinding.ItemErrorStateBinding
import org.koitharu.kotatsu.list.ui.model.ErrorState
import org.koitharu.kotatsu.list.ui.model.ListModel

fun errorStateListAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<ErrorState, ListModel, ItemErrorStateBinding>(
	{ inflater, parent -> ItemErrorStateBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.setOnClickListener {
		listener.onRetryClick(item.exception)
	}

	bind {
		with(binding.textViewError) {
			text = item.exception.getDisplayMessage(context.resources)
			setCompoundDrawablesWithIntrinsicBounds(0, item.icon, 0, 0)
		}
		with(binding.buttonRetry) {
			isVisible = item.canRetry
			setText(item.buttonText)
		}
	}
}
