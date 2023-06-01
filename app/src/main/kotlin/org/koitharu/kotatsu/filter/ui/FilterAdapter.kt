package org.koitharu.kotatsu.filter.ui

import android.content.Context
import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.filter.ui.model.FilterItem
import org.koitharu.kotatsu.list.ui.adapter.listSimpleHeaderAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class FilterAdapter(
	listener: OnFilterChangedListener,
	listListener: ListListener<ListModel>,
) : AsyncListDifferDelegationAdapter<ListModel>(FilterDiffCallback()), FastScroller.SectionIndexer {

	init {
		delegatesManager
			.addDelegate(filterSortDelegate(listener))
			.addDelegate(filterTagDelegate(listener))
			.addDelegate(listSimpleHeaderAD())
			.addDelegate(loadingStateAD())
			.addDelegate(loadingFooterAD())
			.addDelegate(filterErrorDelegate())
		differ.addListListener(listListener)
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		val list = items
		for (i in (0..position).reversed()) {
			val item = list.getOrNull(i) ?: continue
			if (item is FilterItem.Tag) {
				return item.tag.title.firstOrNull()?.toString()
			}
		}
		return null
	}

	companion object {

		const val ITEM_TYPE_HEADER = 0
		const val ITEM_TYPE_SORT = 1
		const val ITEM_TYPE_TAG = 2
	}
}
