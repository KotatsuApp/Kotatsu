package org.koitharu.kotatsu.settings.onboard.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemSourceLocaleBinding
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

fun sourceLocaleAD(
	clickListener: OnListItemClickListener<SourceLocale>
) = adapterDelegateViewBinding<SourceLocale, SourceLocale, ItemSourceLocaleBinding>(
	{ inflater, parent -> ItemSourceLocaleBinding.inflate(inflater, parent, false) }
) {

	binding.root.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		binding.root.text = item.title ?: getString(R.string.other)
		binding.root.isChecked = item.isChecked
	}
}