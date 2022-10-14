package org.koitharu.kotatsu.settings.onboard.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemSourceLocaleBinding
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale
import org.koitharu.kotatsu.utils.ext.textAndVisible

fun sourceLocaleAD(
	listener: SourceLocaleListener,
) = adapterDelegateViewBinding<SourceLocale, SourceLocale, ItemSourceLocaleBinding>(
	{ inflater, parent -> ItemSourceLocaleBinding.inflate(inflater, parent, false) },
) {

	binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
		listener.onItemCheckedChanged(item, isChecked)
	}

	bind {
		binding.textViewTitle.text = item.title ?: getString(R.string.different_languages)
		binding.textViewDescription.textAndVisible = item.summary
		binding.switchToggle.isChecked = item.isChecked
	}
}
