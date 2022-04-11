package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import kotlinx.coroutines.CoroutineScope
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail

class PageThumbnailAdapter(
	dataSet: List<PageThumbnail>,
	coil: ImageLoader,
	scope: CoroutineScope,
	cache: PagesCache,
	clickListener: OnListItemClickListener<MangaPage>
) : ListDelegationAdapter<List<PageThumbnail>>() {

	init {
		delegatesManager.addDelegate(pageThumbnailAD(coil, scope, cache, clickListener))
		setItems(dataSet)
	}
}