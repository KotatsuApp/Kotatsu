package org.koitharu.kotatsu.scrobbling.common.ui.config.adapter

import androidx.core.view.isInvisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemHeaderBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus

fun scrobblingHeaderAD() = adapterDelegateViewBinding<ScrobblingStatus, ListModel, ItemHeaderBinding>(
	{ inflater, parent -> ItemHeaderBinding.inflate(inflater, parent, false) },
) {

	binding.buttonMore.isInvisible = true
	val strings = context.resources.getStringArray(R.array.scrobbling_statuses)

	bind {
		binding.textViewTitle.text = strings.getOrNull(item.ordinal)
	}
}
