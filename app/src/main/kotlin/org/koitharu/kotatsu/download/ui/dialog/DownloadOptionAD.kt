package org.koitharu.kotatsu.download.ui.dialog

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemDownloadOptionBinding

fun downloadOptionAD(
	onClickListener: OnListItemClickListener<DownloadOption>,
) = adapterDelegateViewBinding<DownloadOption, DownloadOption, ItemDownloadOptionBinding>(
	{ layoutInflater, parent -> ItemDownloadOptionBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v -> onClickListener.onItemClick(item, v) }

	bind {
		with(binding.root) {
			title = item.getLabel(resources)
			subtitle = if (item.chaptersCount == 0) null else resources.getQuantityString(
				R.plurals.chapters,
				item.chaptersCount,
				item.chaptersCount,
			)
			setIconResource(item.iconResId)
		}
	}
}
