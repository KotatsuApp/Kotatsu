package org.koitharu.kotatsu.settings.sources.catalog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class SourcesCatalogAdapter(
	listener: OnListItemClickListener<SourceCatalogItem.Source>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.CHAPTER_LIST, sourceCatalogItemSourceAD(coil, lifecycleOwner, listener))
		addDelegate(ListItemType.HINT_EMPTY, sourceCatalogItemHintAD(coil, lifecycleOwner))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return (items.getOrNull(position) as? SourceCatalogItem.Source)?.source?.getTitle(context)?.take(1)
	}
}
