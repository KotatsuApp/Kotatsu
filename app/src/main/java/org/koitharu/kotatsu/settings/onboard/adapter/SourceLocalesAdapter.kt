package org.koitharu.kotatsu.settings.onboard.adapter

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.settings.onboard.model.SourceLocale

class SourceLocalesAdapter(
	clickListener: OnListItemClickListener<SourceLocale>,
) : AsyncListDifferDelegationAdapter<SourceLocale>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(sourceLocaleAD(clickListener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<SourceLocale>() {

		override fun areItemsTheSame(
			oldItem: SourceLocale,
			newItem: SourceLocale,
		): Boolean = oldItem.key == newItem.key

		override fun areContentsTheSame(
			oldItem: SourceLocale,
			newItem: SourceLocale,
		): Boolean = oldItem == newItem
	}
}