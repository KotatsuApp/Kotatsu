package org.koitharu.kotatsu.scrobbling.common.ui.selector.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemMangaListBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga

fun scrobblingMangaAD(
	clickListener: OnListItemClickListener<ScrobblerManga>,
) = adapterDelegateViewBinding<ScrobblerManga, ListModel, ItemMangaListBinding>(
	{ inflater, parent -> ItemMangaListBinding.inflate(inflater, parent, false) },
) {
	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		binding.textViewTitle.text = item.name
		binding.textViewSubtitle.textAndVisible = item.altName
		binding.imageViewCover.setImageAsync(item.cover, null)
	}
}
