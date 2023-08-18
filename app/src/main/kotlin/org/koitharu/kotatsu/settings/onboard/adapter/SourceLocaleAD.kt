package org.koitharu.kotatsu.settings.onboard.adapter

import android.widget.CompoundButton
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.setChecked
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemSourceLocaleBinding
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

fun sourceLocaleAD(
	listener: SourceLocaleListener,
) = adapterDelegateViewBinding<SourceLocale, SourceLocale, ItemSourceLocaleBinding>(
	{ inflater, parent -> ItemSourceLocaleBinding.inflate(inflater, parent, false) },
) {

	val checkedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
		listener.onItemCheckedChanged(item, isChecked)
	}
	binding.switchToggle.setOnCheckedChangeListener(checkedChangeListener)

	bind { payloads ->
		binding.textViewTitle.text = item.title ?: getString(R.string.different_languages)
		binding.textViewDescription.textAndVisible = item.summary
		binding.switchToggle.setOnCheckedChangeListener(null)
		binding.switchToggle.setChecked(item.isChecked, payloads.isNotEmpty())
		binding.switchToggle.setOnCheckedChangeListener(checkedChangeListener)
	}
}
